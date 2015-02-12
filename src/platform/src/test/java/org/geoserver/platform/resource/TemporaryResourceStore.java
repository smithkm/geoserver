package org.geoserver.platform.resource;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class TemporaryResourceStore extends ExternalResource {
    
    File directory;
    FileSystemResourceStore store;
    
    /**
     * Use the given directory
     * @param directory
     * @return
     */
    public static TemporaryResourceStore use(File directory) {
        return new TemporaryResourceStore(directory);
    }
    
    /**
     * Use a temporary copy of the given directory
     * @param template
     * @return
     */
    public static TemporaryResourceStore copy(final File template) {
        return new TemporaryResourceStore(null){
            final TemporaryFolder tmpDir =  new TemporaryFolder();
                @Override
                public Statement apply(Statement base, Description description) {
                    
                    return tmpDir.apply(super.apply(base, description), description);
                }
                @Override
                protected void before() throws Throwable {
                    this.directory = tmpDir.getRoot();
                    if(template!=null) {
                        FileUtils.copyDirectory(template, directory);
                    }
                    super.before();
                }
            
        };
    }
    /**
     * Use an empty temporary directory
     * @param template
     * @return
     */
    public static TemporaryResourceStore temp() {
        return copy((File) null);
    }
    
    /**
     * Use the given temporary folder rule which must be outside this rule.
     * @see org.junit.rules.RuleChain
     * @param template
     * @return
     */
    public static TemporaryResourceStore use(final TemporaryFolder folderRule) {
        return new TemporaryResourceStore(null){
                @Override
                public Statement apply(Statement base, Description description) {
                    
                    return super.apply(base, description);
                }
                @Override
                protected void before() throws Throwable {
                    this.directory = folderRule.getRoot();
                    super.before();
                }
            
        };
    }
    
    private TemporaryResourceStore(File directory) {
        super();
        this.directory = directory;
    }
    
    /**
     * Get the ResourceStore
     * @return
     */
    public FileSystemResourceStore getStore() {
        if(store==null) {
            throw new IllegalStateException();
        }
        return store;
    }
    
    /**
     * Get the directory backing the store.
     * @return
     */
    public File getDirectory() {
        return directory;
    }

    @Override
    protected void before() throws Throwable {
        super.before();
        store = new FileSystemResourceStore(directory);
    }

    @Override
    protected void after() {
        try {
            store.destroy();
        } catch (Exception e) {
            throw new AssertionError(e);
        } finally {
            store = null;
            super.after();
        }
    }
    
}
