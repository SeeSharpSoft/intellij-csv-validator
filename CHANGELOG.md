# Changelog

## Unreleased

### Added

### Changed

### Removed

### Fixed

## [4.0.1] - Dec 08, 2024

### Added

- Localization support

### Changed

- Upgrade to gradle 8.10
- Improve check for CsvFile
- Code cleanup

### Fixed

- Read access is allowed from inside read-action only #878
- Failsafe acceptCsvFile check #882
- Use ProjectActivity instead of StartupActivity

## 4.0.0 - Oct 07, 2024

### Added
- Tabularize formatting is back!

### Changed
- Text editor is shown first by default

## 3.4.0 - Aug 10, 2024

### Fixed

- ActionUpdateThread.OLD_EDT is deprecated and going to be removed soon
- Cannot create class CsvEditorSettingsProvider

## 3.3.0 - Feb 24, 2024

### Fixed

- Update gradle build
- PluginException: xxx ms to call on EDT CsvChangeSeparatorActionGroup#update@EditorPopup #401
- AlreadyDisposedException: Already disposed #639
- Exceptions occurred on invoking the intention 'Unquote' on a copy of the file #670 #816

### Fixed

- StringIndexOutOfBoundsException: begin 0, end -1, length 5993 #801
- Unhandled exception in [CoroutineName(PsiAwareFileEditorManagerImpl)] #666

## 3.2.3 - Nov 05, 2023

### Added

- Prevent github issue submitter spam

### Fixed

- Improve issue duplicate finder

## 3.2.2 - Oct 14, 2023

### Fixed

- NullPointerException: Cannot invoke "getSelectedColumn()" because "tblEditor" is null #519
- IllegalStateException: Attempt to modify PSI for non-committed Document! #516

- StringIndexOutOfBoundsException: begin 0, end -1, length 5995 #511
- ArrayIndexOutOfBoundsException: 12 >= 12 #482

## 3.2.1 - Jul 08, 2023

### Added

- add support for upcoming IntelliJ version 2023.2.*

### Fixed

- broken tests

## 3.2.0 - May 12, 2023

### Fixed

- Argument for @NotNull parameter 'tableEditor' of CsvTableActions.adjustColumnWidths must not be null #431
- NullPointerException #429

- Comment handling breaking in table editor #451

### Added

- prevent error report flooding

## 3.1.0 - Feb 27, 2023

### Fixed

- NullPointerException: Cannot invoke "URL.toExternalForm()" because "location" is null #424
- java.lang.ArrayIndexOutOfBoundsException: 0 >= 0 #418

## 3.0.3 - Feb 10, 2023

### Fixed

- Argument for @NotNull parameter 'anchor' of CsvPsiTreeUpdater.appendField must not be null #392
- ArrayIndexOutOfBoundsException: 8 >= 8 #396

- catch unreasonable exception when retrieving service #410
- NullPointerException: Cannot invoke "CsvTableEditor.getActions()" because the return value of "CsvTableEditorActions.getTableEditor(AnActionEvent)" is null #394

### Added

- 358 ms to call on EDT CsvChangeSeparatorActionGroup#update@EditorPopup #401
## 3.0.2 - Dec 15, 2022

### Fixed

- Detected bulk mode status update from DocumentBulkUpdateListener #384
- Argument for @NotNull parameter 'parent' of PsiHelper.getNthChildOfType must not be null #372

- Argument for @NotNull parameter 'element' of PsiHelper.getSiblingOfType must not be null #375
- Cannot invoke "Document.getText()" because "document" is null #388

### Fixed

- Cannot invoke "PsiFile.getProject()" because the return value of "CsvPsiTreeUpdater.getPsiFile()" is null #378
- Argument for @NotNull parameter 'replacement' of CsvPsiTreeUpdater$ReplacePsiAction.<init> must not be null #380

- provide project parameter for opening link
- Cannot invoke "Document.insertString(int, java.lang.CharSequence)" because "document" is null #386

### Fixed

- first extension sanity check

## 3.0.1 - Nov 12, 2022

### Fixed

- cannot init component state (componentName=CsvFileAttributes) #359
- cannot invoke "add(Object)" because "this.myUncommittedActions" is null #361
- cannot invoke "createNotification(...)" because "notificationGroup" is null #362
- cannot invoke "getManager()" because the return value of "getPsiFile()" is null #363

### Fixed

- image in plugin description
- plugin update restart

## 3.0.0 - Nov 09, 2022

MAJOR UPDATE VERSION 3

General
-------

- renamed plugin to 'CSV Editor'
- fixed all compatibility issues with respect to IntelliJ platform 2022.*
- rework language lexer
- simplification of formatter & remove 'Tabularize' formatting
- remove slow & (useless) structure view
- adjusted setting dialogs
- integrated GitHub issue reporter in case plugin raises an exception
- removed TSV & PSV language, only CSV language but different filetypes

Table Editor
------------

- use PSI Tree as data source
- integrate with native IntelliJ IDE document change handler (e.g. for undo/redo)
- simplify UI/UX & remove header toolbar
- support showing and editing comment lines
- auto adjust row width
- manually adjust row height via dragging
- always use first line for header/column text

## 2.21.0 - Oct 26, 2022

### Changed

- support comments in fast lexer

### Changed

- reworked (rainbow) coloring

### Changed

- avoid formatting while typing

### Changed

- limit column highlighting to 1000 entries around caret

### Changed

- limit calculation and buffering of CSV column info data

### Fixed

- short comments

## 2.20.0 - Oct 24, 2022

### Fixed

- Cannot load from object array because "data" is null #335 #337

- Empty comment indicator

### Added

- Support fast lexing for default comments
- Simplify & unify both lexers

## 2.19.0 - Jul 24, 2022

### Added

- Ability to auto-reload changed files #316

### Fixed

- NullPointerException when applying editor state #327
- additional disposed checks

## 2.18.2 - Dec 14, 2021

### Fixed

- java.lang.NullPointerException #320
- Exception when Show info balloon is not selected #318

## 2.18.1 - Nov 04, 2021

### Fixed

- set require-restart attribute

### Added

- Ability to open urls from IntelliJ CSV view #312

## 2.18.0 - Oct 21, 2021

NOTE: Minimum version requirement changed to v2020.1 and newer

### Fixed

- Show diff opens an empty window #306

## 2.17.1 - Jun 30, 2021

### Fixed

- Plugin not showing column at caret Ctr+F1 #300
- Past few versions of this plugin don't show colors #298

## 2.17.0 - Jun 21, 2021

### Added

- Plugin name ### Changed
- CSV

### Fixed

- Null pointer when using csv rendering in Markdown documentation #292
- NullPointerException on startup #295

## 2.16.4 - May 27, 2021

### Fixed

- fully prevent calculateDistributedColumnWidth on erroneous CSV #283
- Memory leak of PsiFile #284

## 2.16.3 - Apr 24, 2021

### Fixed

- Exception in CSV plugin when using with remote SSH host #279

## 2.16.2 - Apr 21, 2021

### Fixed

- Editor complaining about crashes on PyCharm 2021.1 #274

## 2.16.1 - Apr 20, 2021

### Fixed

- keep existing & correct entries in CSV attributes map

## 2.16.0 - Apr 18, 2021

### Added

- Ability to split on ASCII separator character \x1e #267
- "General" settings group

### Fixed

- Lower annotator severity to not appear as problem
- Prevent non CSV entries in CSV attributes map #268

## 2.15.1 - Mar 25, 2021

### Fixed

- Rainbow Values no longer works #265

## 2.15.0 - Mar 23, 2021

### Added

- Default value separator #259

### Fixed

- Removal of deprecated function usage

## 2.14.4 - Feb 14, 2021

### Added

- Auto detect value separator (by count)

## 2.14.3 - Oct 10, 2020

### Added

- Added default "Header row fixed" setting
- Support "Comment with line comment" #247

### Fixed

- "Value coloring" change not applied to open files

## 2.14.2 - Sep 17, 2020

### Fixed

- Settings reset every update #245
- Removing comment indicator causes parsing errors

## 2.14.1 - Aug 14, 2020

### Fixed

- Performance for indexing large CSV files #239

## 2.14.0 - Aug 04, 2020

### Added

- Predefined column colors (Rainbow-style)
- Enhanced color scheme switch

### Added

- Table Editor coloring

## 2.13.0 - Jul 20, 2020

### Added

- Support for customizable line comments ('#' indicates a line comment per default)

## 2.12.0 - Jun 14, 2020

### Added

- flexible settings format for value separator & escape character

### Fixed

- no accessors for class CsvValueSeparator #221
- Default Value Separator get frequently reset to comma, changes only applied after restart #222

## 2.11.1 - May 9, 2020

### Fixed

- Memory issues and freezes after updating #204

## 2.11.0 - May 8, 2020

### Added

- Adding custom separator #177

## 2.10.0 - Apr 19, 2020

### Fixed

- resolve CSV/TSV file type conflict for versions 2020.* with Database plugin

## 2.9.3 - Mar 08, 2020

### Added

- option to keep trailing spaces for CSV/TSV/PSV files

### Fixed

- consider escape char inside quotes as escaped text

## 2.9.2 - Feb 24, 2020

### Fixed

- Backslash in text is considered a special character #184
- NullPointerException thrown when trying to view CSV as table #185

## 2.9.1 - Feb 15, 2020

### Fixed

- update failed for AnAction #181
- selection indicator for default separator action

## 2.9.0 - Feb 07, 2020

### Added

- customizable escape character #159
- value separator setting moved from 'Code Style' to 'General'

### Added

- lots of cleanup & rework

## 2.8.2 - Jan 22, 2020

### Fixed

- horizontal scrolling within table editor #169

## 2.8.1 - Nov 22, 2019

### Fixed

- vertical scrolling within table editor #164

## 2.8.0 - Oct 12, 2019

### Added

- improved font handling in table editor #122

### Fixed

- proper font handling in balloon tooltips

## 2.7.1 - Sep 26, 2019

Support for IDE v192.*

## 2.7.0 - Sep 16, 2019

### Added

- add separator selection to table editor #140

### Fixed

- coloring of table cells (e.g. selection mode)
- enter edit mode via keyboard (e.g. ENTER key in cell)

## 2.6.4 - Aug 30, 2019

### Fixed

- scrollable table cells
- auto cell height computation

## 2.6.3 - Aug 25, 2019

### Fixed

- Index out of bound error on multi line clear cells #151

## 2.6.2 - Aug 09, 2019

### Fixed

- AssertionError: Already disposed: Project (Disposed) #147
- No fallback font used in table editor #145

## 2.6.1 - Aug 01, 2019

### Added

- plugin logo icons added (Thx to FineVisuals for support)

## 2.6.0 - Jul 18, 2019

### Added

- Table column width calculation and adjustment based on content

## 2.5.1 - Jun 25, 2019

### Fixed

- ConcurrentModificationException in MultiLineCellRenderer

## 2.5.0 - May 16, 2019

### Added

- PSV file support

### Fixed

- NullPointerException in StorageHelper class

## 2.4.0 - May 11, 2019

### Added

- option to keep/ignore a linebreak at the end of a file (table editor)
- improved change detection of table editor to avoid overwriting original text representation without editing any values

### Added

- file based value separator (e.g. ',' or ';')

## 2.3.1 - Mar 31, 2019

### Added

- use default color scheme & font for table editor as well

### Fixed

- ConcurrentModificationException tackled (table editor)

## 2.3.0 - Mar 04, 2019

### Added

- Zoom table-editor cells with Ctrl+Mouse Wheel (contribution by @royqh1979)

### Fixed

- Scratches are now recognised as CSV
- Several issues resolved by reworking column/row editing

## 2.2.1 - Jan 26, 2019

### Fixed

- ArrayIndexOutOfBoundsException when opening a CSV file with an unexpected separator (table editor)
- ConcurrentModificationException during event handling (table editor)

## 2.2.0 - Jan 13, 2019

### Added

- option to fixate first row as header (table editor)
- row numbers (table editor)

### Fixed

- exception when clicking to right of populated columns (table editor)
- keep leading/trailing whitespaces (table editor)

## 2.1.0 - Jan 01, 2019

### Added

- support column highlighting for table editor
- support all kind of text attributes for column highlighting

### Added

- table editor values not longer enforced to be quoted on save (customizable)

### Fixed

- prevent backspace/delete erasing cell value while editing (table editor)

## 2.0.2 - Dec 16, 2018

### Fixed

- top and bottom panel scrollable & add column/row buttons removed

## 2.0.1 - Dec 02, 2018

### Fixed

- disabling table editor for large files
- applying row height for newly opened files

## 2.0.0 - Nov 20, 2018

### Added

- CSV/TSV table editor!!!

## 1.9.1 - Oct 12, 2018

### Fixed

- reading/writing CSV editor states
- tooltip for tab separator if disabled

## 1.9.0 - Oct 01, 2018

### Added

- CSV/TSV editor settings (File > Settings > General > CSV/TSV Editor)
- TAB (separator) highlighting

### Added

- Enable/disable balloon info
- Soft wrap settings specific for CSV/TSV

## 1.8.2 - Sep 20, 2018

### Added

- Customizable column coloring (File > Settings > Editor > Color Scheme > CSV)

## 1.8.1 - Aug 16, 2018

### Added

- East Asian full-width character support for 'Tabularize' (optional) - disabled by default due to lower performance

## 1.8.0 - Jul 05, 2018

### Added

- Custom 'Wrapping' settings
- Column highlighter takes whitespaces into account
- several code & performance improvements

### Changed

- Column highlighting only happens on selection

### Fixed

- CSV column info tooltip trumps spellchecker tooltip (but keeps the visualization of a typo)
- Show tooltip even when caret is at the last position withing the CSV file
- Support for suppressing inspections not relevant for CSV (e.g. 'Problematic Whitespace')
- Structure View: proper handling of elements (instead of endless loading)

## 1.7.0 - Jun 19, 2018

### Added

- Annotated values: tooltip shows the value itself, the header and the column index
- Active column (caret position) is colored differently
- several code & performance improvements

### Fixed

- 'Add separator' inspection is adding correct separator (e.g. tabs in TSV files)

## 1.6.1 - May 11, 2018

### Added

- New icons

## 1.6.0 - Apr 02, 2018

### Added

- TSV file support
TSV files a recognized as such but treated as a variant of CSV files, the same syntax highlighting and code style settings are applied.

### Added

- tab (↹) and pipe (|) as separators added
- spellchecker enabled

### Added

- it was necessary to increase the minimum IDE version from 2016.1.1 to 2016.3.2 due to a required fix in the formatting code. Previous versions of the plugin can still be downloaded directly from Jetbrains Plugin Repository.

## 1.5.1 - Mar 21, 2018

### Added

- intentions to shift a whole column left/right
(includes bugfix for breaking intention functionality on previous IDE versions)

## 1.5.0 - Feb 10, 2018

### Added

- intentions to shift a whole column left/right

## 1.4.1 - Jan 19, 2018

Handle tabs as 1-length character - fixes 'Tabularize' for csv files with tabs
Fix default settings initialization
