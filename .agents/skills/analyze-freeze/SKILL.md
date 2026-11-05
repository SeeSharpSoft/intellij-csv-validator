---
name: analyze-freeze
description: Figures out what's going on in your thread dump and explains the cause of UI freeze for JetBrains IDEs
---

# When analyzing a thread dump:

1. Your domain is JetBrains IDE, written in Java and Kotlin, using IntelliJ Platform. UI is built with Java Swing.
2. Identify the event dispatch (AWT / EDT) thread, it usually has "AWT-EventQueue-0" or "EDT" in the name.
3. Identify threads that have read lock; they have one of the following frames: `com.intellij.openapi.application.impl.ApplicationImpl.runReadAction`, `com.intellij.openapi.application.impl.ApplicationImpl.tryRunReadAction`, `com.intellij.platform.locking.impl.NestedLocksThreadingSupport.runWriteIntentReadAction`, `com.intellij.platform.locking.impl.NestedLocksThreadingSupport.tryRunWriteIntentReadAction` in stack.
4. Identify the thread having write action; it has `com.intellij.openapi.application.impl.ApplicationImpl.runWriteAction` in stack.

# APIs and threading contracts

File input/output (IO) operations include `java.nio.file.*` or `java.io.*` or `sun.nio.fs.`.

Common networking libraries have the following frames: `sun.net.NetworkClient.doConnect`, `org.apache.http.impl.client.CloseableHttpClient.execute`, `org.apache.http.impl.client.CloseableHttpClient.execute`, `org.apache.http.impl.io.SessionInputBufferImpl.read`, `okhttp3.internal.connection.RealCall.execute`.

Read actions with `com.intellij.openapi.application.impl.ApplicationImpl.runReadAction` in stack usually are not cancellable. `com.intellij.openapi.application.ReadAction.compute`, `com.intellij.openapi.application.ReadAction.run`, `com.intellij.openapi.application.ActionsKt.runReadAction` are common causes. But, in case stack includes `com.intellij.openapi.application.impl.ApplicationImpl.tryRunReadAction` or `com.intellij.openapi.application.rw.InternalReadAction.tryReadCancellable` wrapping a read action then such read action is properly cancellable and the cause of a UI freeze is probably somewhere else. When thread stack starts with `java.lang.Object.wait` or `com.intellij.platform.locking.impl.NestedLocksThreadingSupport.smartAcquireReadPermit` for a thread asking for read action it means this thread does not hold read lock yet, but waits for it.

Threads having read lock are the main suspects of a UI freeze.

A thread performing actual init of service may be identified by `com.intellij.serviceContainer.ServiceInstanceInitializer.createInstance` frame. The stack of a thread getting a service instance will include `com.intellij.serviceContainer.ComponentManagerImpl.getService`, it will use `kotlinx.coroutines.BlockingCoroutine.joinBlocking` to await init.

Virtual File System (VFS) operations include `com.intellij.openapi.vfs.newvfs.*`. Such as `com.intellij.openapi.vfs.newvfs.impl.VirtualDirectoryImpl.getChildren` or `com.intellij.openapi.vfs.VfsUtilCore.visitChildrenRecursively`.

Java classloading operations include `com.intellij.util.lang.ClassPath.findClass` or `java.lang.ClassLoader.loadClass`.

External processes are started with `com.intellij.execution.configurations.GeneralCommandLine.createProcess` or `java.lang.ProcessBuilder.start`. Threads waiting for external processes to finish are identified by `com.intellij.execution.process.ProcessHandler.waitFor` in stack.

Computing a FileType of a virtual file is an expensive operation when accessed with `com.intellij.openapi.vfs.VirtualFile.getFileType`. It can be detected by `com.intellij.openapi.fileTypes.impl.FileTypeDetectionService.getOrDetectFromContent` frame.

Reading Region settings via `com.intellij.ide.RegionSettings.getRegion` is an expensive operation, because it does IO and or accesses Windows registry.

UI action presentations are updated by `com.intellij.openapi.actionSystem.ex.ActionUtil.updateAction`. Action update is a very frequent operation and it must be fast. It may not wait for services init, IO, network, heavy computations because performed under read action.

Matching regular expressions `java.util.regex.*` is a non-cancellable operation, e.g. `java.util.regex.Matcher.matches`.

Parsing JSON/YAML via `com.fasterxml.jackson.` is a non-cancellable operation.

Services performing `getInstance()` of another service in their constructor/init may cause eager early init of this service. This contributes to a UI freeze.

Coroutine `runBlocking` call inside read action is a source of a UI freeze. It is not cancellable, developers must use `runBlockingCancellable` instead. Nonetheless, `runBlockingCancellable` and `runBlockingMayBeCancellable` is still sensitive to thread starvation, in this case it will block on a coroutine `kotlinx.coroutines.BlockingCoroutine` and this coroutine usually will be in `{Cancelling}` state.

# Coroutine dispatchers and Dispatchers.Default starvation

`Dispatchers.Default` (CPU pool, size = number of CPU cores) and `Dispatchers.IO` share the same threads named `DefaultDispatcher-worker-N`. Free or parked `DefaultDispatcher-worker` threads do NOT mean `Dispatchers.Default` has free slots — IO and Default tasks are indistinguishable by thread name.

A thread executing a Default (CPU) task has `kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.runDefaultDispatcherTask` in stack. To detect Default starvation, count threads that have this frame AND are blocked (`WAITING`/`TIMED_WAITING` in `CompletableFuture.join`, `ProcessHandler.waitFor`, `Semaphore.waitFor`, selector `select()`, file/network IO). If this count is close to the number of CPU cores, `Dispatchers.Default` is exhausted: every coroutine needing Default dispatch (including continuations returning from `withContext(Dispatchers.EDT) {}` blocks and flow collectors) is stalled in the queue even though the rest of the IDE looks idle.

`kotlinx.coroutines.internal.LimitedDispatcher$Worker` and `kotlinx.coroutines.internal.SoftLimitedDispatcher$Worker` frames mean `Dispatchers.Default.limitedParallelism(...)` — blocking these threads also consumes Default CPU slots.

Coroutine dump state semantics:

* `CREATED` — never started; if many, suspect dispatcher starvation.
* `SUSPENDED` — parked at the suspension point shown in its frames.
* `RUNNING` with a thread executing matching frames — actually running, inspect that thread.
* `RUNNING` with NO thread executing it — its `withContext` block already completed on another dispatcher (often EDT) and the continuation resume is queued into a starved dispatcher. No "suspended" probe event fires when an inner `withContext` block completes, so the state stays `RUNNING`. This is a strong Dispatchers.Default starvation signal, not a dump inconsistency.

# Common causes of a UI freeze:

1. EDT is blocked by a long-running read action, in this case it does `com.intellij.platform.locking.impl.RunSuspend.await` and tries to pump EDT queue. One of the background threads has read action and does not check for write action pending.
2. EDT is blocked by network IO.
3. EDT is blocked by file IO.
4. EDT is performing VFS operations, such as `com.intellij.openapi.vfs.impl.VirtualFileManagerImpl.findByUrl`.
5. EDT is performing PSI operations, such as `com.intellij.psi.impl.file.impl.FileManagerImpl.findFile`.
6. EDT is performing index access, such as `com.intellij.psi.stubs.StubIndex.getElements` or `com.intellij.util.indexing.FileBasedIndexEx.ensureUpToDate`.
7. EDT is blocked by heavy classloading.
8. EDT accessing PasswordSafe `com.intellij.ide.passwordSafe.impl.BasePasswordSafe.get` which is heavy blocking operation
9. EDT is waiting for a service lock, then it is usually stuck in `getInstance()` call.
10. EDT is waiting for write intent lock, while one of the background threads has read action and waiting for another service to init.
11. EDT is blocked by waiting for an external process to finish.
12. EDT is blocked by the start of an external process.
13. VFS listener is performing expensive operations, such as `com.intellij.openapi.vfs.VfsUtilCore.visitChildrenRecursively`.
14. Service initialization waits for IO
15. Service initialization waits for network IO
16. Service initialization waits for another service to init
17. Service initialization waits for an external process to start
18. Service initialization waits for an external process to finish
19. Computing FileType on EDT or from VFS listener.
20. Reading Region settings on EDT or from the action update process.
21. Waiting for a service init from the action update process.
22. Thread starvation in a coroutine `runBlocking` or `runBlockingCancellable` call.
23. EDT waits for a background coroutine while pumping events: EDT parked in `java.awt.EventQueue.getNextEvent` under `com.intellij.openapi.progress.impl.PlatformTaskSupportKt.pumpEventsForHierarchy` (e.g. from `com.intellij.openapi.fileEditor.impl.FileEditorManagerImplKt.blockingWaitForCompositeFileOpen` waiting for `EditorComposite.waitForAvailable`). The EDT itself is healthy and idle; the cause is whatever prevents the awaited job from completing — check Dispatchers.Default starvation first.
24. Dispatchers.Default pool exhausted by coroutines doing blocking calls (`java.util.concurrent.CompletableFuture.join`/`get`, `ProcessHandler.waitFor`, blocking IO, selector `select()`), so unrelated coroutines cannot be dispatched or resumed.

Important exceptions:

* A thread running under modal synchronous progress may not be considered having non-cancellable read action; you can identify it by frames starting with `com.intellij.openapi.progress.impl.CoreProgressManager.lambda$runProcessWithProgressSynchronously` in stack
* Read actions on the EDT thread directly are never cancellable
* Nested non-cancellable read action calls inside a cancellable read action call are always cancellable
* Threads of `com.intellij.codeInsight.daemon.impl.PassExecutorService` are properly cancellable and do not block Write action unless perform IO, network or regular expressions matching.

# Appropriate fixes:

* Move VFS code out of EDT
* Move PSI access out of EDT
* Move index access out of EDT
* Move network code out of EDT
* Move IO out of EDT
* Start external processes on background threads
* Wait for external processes to finish on background threads
* Move network code out of a Read or Write action
* Move network code out of service initialization
* Move IO code out of service initialization
* Move external process start out of service initialization
* Move external process waiting out of service initialization
* Use `com.intellij.openapi.vfs.DiskQueryRelay` to simulate cancellable IO under read action
* Use `ReadAction.nonBlocking {}.executeSynchronous()` or coroutines APIs `readAction`, `smartReadAction` on background to restart long read actions when a Write action is pending
* Do not wait for locks / synchronization in EDT
* Use `awaitWithCheckCancelled {}` to wait for locks / synchronization under Read action
* Use `computeDetached {}` to simulate cancellable IO and do not exhaust default pool
* Use backgroundable VFS listeners: `BulkFileListenerBackgroundable` and `VirtualFileManager.addAsyncFileListenerBackgroundable`
* Use more lazy initialization to avoid heavy classloading, preload critical services on project start via `ProjectActivity` or a background code
* Remember to check cancellation status via `ProgressManager.checkCanceled()`
* Use `com.intellij.openapi.util.text.StringUtil#newBombedCharSequence` for cancellable Regex matching
* Use `getServiceIfCreated()` instead of `getInstance()` to avoid service init from action update
* Use lazy application and project listeners in plugin.xml and `MessageBus` topics to avoid eager service init
* Never block inside `Dispatchers.Default` coroutines: replace `CompletableFuture.join()`/`get()` with `asDeferred().await()`, move process waits, blocking IO, and selectors to `Dispatchers.IO` or `withContext(Dispatchers.IO)`

# Output

Your explanation should include the causing code frame, threads participating in a freeze, a problematic service if any, probable plugin causing the freeze.