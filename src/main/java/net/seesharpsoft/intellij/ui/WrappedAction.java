package net.seesharpsoft.intellij.ui;

import java.awt.event.*;
import java.beans.*;
import javax.swing.*;

/*
 *  The WrappedAction class is a convenience class that allows you to replace
 *  an installed Action with a custom Action of your own. There are two
 *  benefits to using this class:
 *
 *  a) Key Bindings of the original Action are retained for the custom Action.
 *  b) the original Action is retained so your custom Action can invoke the
 *     original Action.
 *
 *  This class is abstract so your custom Action must extend this class and
 *  implement the actionPerformed() method.
 *
 * Source: https://tips4java.wordpress.com/2008/11/04/table-tabbing/
 */
abstract class WrappedAction implements Action
{
    private Action originalAction;
    private JComponent component;
    private Object actionKey;

    /*
     *  Replace the default Action for the given KeyStroke with a custom Action
     */
    public WrappedAction(JComponent component, KeyStroke keyStroke)
    {
        this.component = component;
        Object actionKey = getKeyForActionMap(component, keyStroke);

        if (actionKey == null)
        {
            String message = "no input mapping for KeyStroke: " + keyStroke;
            throw new IllegalArgumentException(message);
        }

        setActionForKey( actionKey );
    }

    /*
     *  Replace the default Action with a custom Action
     */
    public WrappedAction(JComponent component, Object actionKey)
    {
        this.component = component;
        setActionForKey( actionKey );
    }

    /*
     *  Search the 3 InputMaps to find the KeyStroke binding
     */
    private Object getKeyForActionMap(JComponent component, KeyStroke keyStroke)
    {
        for (int i = 0; i < 3; i++)
        {
            InputMap inputMap = component.getInputMap(i);

            if (inputMap != null)
            {
                Object key = inputMap.get(keyStroke);

                if (key != null)
                    return key;
            }
        }

        return null;
    }

    /*
     *  Replace the existing Action for the given action key with a
     *  wrapped custom Action
     */
    private void setActionForKey(Object actionKey)
    {
        //  Save the original Action

        this.actionKey = actionKey;
        originalAction = component.getActionMap().get(actionKey);

        if (originalAction == null)
        {
            String message = "no Action for action key: " + actionKey;
            throw new IllegalArgumentException(message);
        }

        //  Replace the existing Action with this class

        install();
    }

    /*
     *  Child classes should use this method to invoke the original Action
     */
    public void invokeOriginalAction(ActionEvent e)
    {
        originalAction.actionPerformed(e);
    }

    /*
     *  Install this class as the default Action
     */
    public void install()
    {
        component.getActionMap().put(actionKey, this);
    }

    /*
     *	Restore the original Action as the default Action
     */
    public void unInstall()
    {
        component.getActionMap().put(actionKey, originalAction);
    }
    //
//  Delegate the Action interface methods to the original Action
//
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        originalAction.addPropertyChangeListener(listener);
    }

    public Object getValue(String key)
    {
        return originalAction.getValue(key);
    }

    public boolean isEnabled()
    {
        return originalAction.isEnabled();
    }

    public void putValue(String key, Object newValue)
    {
        originalAction.putValue(key, newValue);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        originalAction.removePropertyChangeListener(listener);
    }

    public void setEnabled(boolean newValue)
    {
        originalAction.setEnabled(newValue);
    }
    //
//  Implement some AbstractAction methods
//
    public Object[] getKeys()
    {
        if (originalAction instanceof AbstractAction)
        {
            AbstractAction abstractAction = (AbstractAction)originalAction;
            return abstractAction.getKeys();
        }

        return null;
    }

    public PropertyChangeListener[] getPropertyChangeListeners()
    {
        if (originalAction instanceof AbstractAction)
        {
            AbstractAction abstractAction = (AbstractAction)originalAction;
            return abstractAction.getPropertyChangeListeners();
        }

        return null;
    }
}
