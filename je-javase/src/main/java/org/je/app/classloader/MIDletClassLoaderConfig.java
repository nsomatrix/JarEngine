package org.je.app.classloader;

import java.util.List;
import java.util.Vector;

import org.je.app.ConfigurationException;

public class MIDletClassLoaderConfig {

    public static final int DELEGATION_STRICT = 0;

    public static final int DELEGATION_RELAXED = 1;

    public static final int DELEGATION_DELEGATING = 2;

    public static final int DELEGATION_SYSTEM = 3;

    private int delegationType;

    private boolean delegationSelected;

    List appclasses = new Vector();

    List appclasspath = new Vector();

    public MIDletClassLoaderConfig() {
        delegationSelected = false;
        delegationType = DELEGATION_STRICT;
    }

    public void setDelegationType(String delegationType) throws ConfigurationException {
        if ("strict".equalsIgnoreCase(delegationType)) {
            this.delegationType = DELEGATION_STRICT;
        } else if ("relaxed".equalsIgnoreCase(delegationType)) {
            this.delegationType = DELEGATION_RELAXED;
        } else if ("delegating".equalsIgnoreCase(delegationType)) {
            this.delegationType = DELEGATION_DELEGATING;
        } else if ("system".equalsIgnoreCase(delegationType)) {
            if ((appclasses.size() != 0) || (appclasspath.size() != 0)) {
                throw new ConfigurationException("Can't extend system CLASSPATH");
            }
            this.delegationType = DELEGATION_SYSTEM;
        } else {
            throw new ConfigurationException("Unknown delegationType [" + delegationType + "]");
        }
        delegationSelected = true;
    }

    public int getDelegationType(boolean forJad) {
        if ((!delegationSelected) && (!forJad)) {
            return DELEGATION_RELAXED;    
        } else {
            return delegationType;
        }
    }
    
    public boolean isClassLoaderDisabled() {
        return (this.delegationType == DELEGATION_SYSTEM);
    }

    public void addAppClassPath(String path) throws ConfigurationException {
        if (this.delegationType == DELEGATION_SYSTEM) {
            throw new ConfigurationException("Can't extend system CLASSPATH");
        }
        appclasspath.add(path);
    }

    public void addAppClass(String className) throws ConfigurationException {
        if (this.delegationType == DELEGATION_SYSTEM) {
            throw new ConfigurationException("Can't extend system CLASSPATH");
        }
        appclasses.add(className);
    }

}
