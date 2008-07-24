/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Power*Architect.
 *
 * Power*Architect is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*Architect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package ca.sqlpower.architect.ddl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;

import ca.sqlpower.architect.SQLColumn;
import ca.sqlpower.architect.SQLObject;

/**
 * An instance of this class displays the warning message associated with an
 * object's property, and allows user to actively modify the value of the
 * property to avoid possible SQL failures.
 * 
 * By default, if not specified by the warning. An instance would modify the
 * name of the object.
 */
public class ObjectPropertyModificationDDLComponent extends GenericDDLWarningComponent {

    private static final Logger logger = Logger.getLogger(ObjectPropertyModificationDDLComponent.class);
    
    /**
     * This DDL warning specifies the property to be modified by this
     * component.
     */
    private final DDLWarning warning;
    
    private String propertyName;
    
    private JComponent component;
    
    /**
     * List of text fields that correspond to the property of each
     * SQLObject in the list of involved objects for the warning
     * this component holds. The property is determined by the warning
     * type an instance of this class carries.
     */
    final Map<JTextField, SQLObject> textFields = new Hashtable<JTextField, SQLObject>();
    
    private Runnable changeApplicator;

    public ObjectPropertyModificationDDLComponent(DDLWarning warning) {
        super(warning);
        
        logger.debug("Creating warning component for " + warning);

        this.warning = warning;
        propertyName = warning.getQuickFixPropertyName();
        this.changeApplicator = new Runnable() {
            public void run() {
                logger.debug("Now attempt to modify object property");
                for (Map.Entry<JTextField, SQLObject> entry : textFields.entrySet()) {
                    try {
                        SQLColumn col = (SQLColumn)(entry.getValue());
                        Method setter = PropertyUtils.getWriteMethod(PropertyUtils.getPropertyDescriptor(col, propertyName));
                        setter.invoke(col, entry.getKey().getText());
                    } catch (NoSuchMethodException e) {
                        logger.error("Corresponding mutator method for property: " + propertyName + "was not found.\n" +
                        "Now attempting to modify object's name instead.");
                        entry.getValue().setName(entry.getKey().getText());
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                        throw new IllegalArgumentException();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        throw new RuntimeException();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                        throw new RuntimeException();
                    }
                }
            }
        };
        component = new JPanel(); 
        component.add(getQuickFixButton());                //XXX anti-pattern
        component.add(new JLabel(warning.getMessage()));
        if (warning.getQuickFixPropertyName() != null) {
            component.add(new JLabel(" Change " + warning.getQuickFixPropertyName() + ": "));
            List<? extends SQLObject> list = warning.getInvolvedObjects();
            for (SQLObject obj : list) {
                JTextField jtf = new JTextField();
                jtf.setColumns(5);
                try {
                    Method getter = PropertyUtils.getReadMethod(PropertyUtils.getPropertyDescriptor(obj, propertyName));
                    jtf.setText((String)(getter.invoke(obj)));
                    logger.debug("Successfully modified object's property.");
                } catch (NoSuchMethodException e) {
                    logger.error("Corresponding accessor method for property: " + warning.getQuickFixPropertyName() + "was not found.\n" +
                    "Now setting text field text to be object's name instead.");
                    jtf.setText(obj.getName());
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    throw new IllegalArgumentException();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    throw new RuntimeException();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                    throw new RuntimeException();
                }
                component.add(jtf);
                textFields.put(jtf, obj);
            }
        }
    }

    public void applyChanges() {
        changeApplicator.run();
    }

    public Runnable getChangeApplicator() {
        return changeApplicator;
    }

    public JComponent getComponent() {
        return component;
    }

    public DDLWarning getWarning() {
        return warning;
    }
}
