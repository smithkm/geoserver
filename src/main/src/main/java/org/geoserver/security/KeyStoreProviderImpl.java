/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.security.password.RandomPasswordProvider;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.BeanNameAware;

import static org.geoserver.security.SecurityUtils.toBytes;

/**
 * Class for Geoserver specific key management
 * 
 * <strong>requires a master password</strong> form
 * {@link MasterPasswordProviderImpl}
 * 
 * The type of the keystore is JCEKS and can be used/modified
 * with java tools like "keytool" from the command line.
 *  *  
 * 
 * @author christian
 *
 */
public class KeyStoreProviderImpl implements BeanNameAware, KeyStoreProvider{
    
    public final static String DEFAULT_BEAN_NAME="DefaultKeyStoreProvider";
    public final static String DEFAULT_FILE_NAME="geoserver.jceks";
    public final static String PREPARED_FILE_NAME="geoserver.jceks.new";
    
    public final static String CONFIGPASSWORDKEY = "config:password:key";
    public final static String URLPARAMKEY = "url:param:key";
    public final static String USERGROUP_PREFIX = "ug:";
    public final static String USERGROUP_POSTFIX = ":key";
    
    static protected Logger LOGGER = Logging.getLogger("org.geoserver.security");
    protected String name;
    protected Resource keyStoreResource;
    protected KeyStore ks;

    public final static String KEYSTORETYPE = "JCEKS";
     
    GeoServerSecurityManager securityManager;

    public KeyStoreProviderImpl()  {
    }

    @Override
    public void setBeanName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setSecurityManager(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public GeoServerSecurityManager getSecurityManager() {
        return securityManager;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.password.KeystoreProvider#getKeyStoreProvderFile()
     */
    @Override
    public Resource getResource() {
        if (keyStoreResource == null) {
            keyStoreResource = securityManager.security().get(DEFAULT_FILE_NAME);
        }
        return keyStoreResource;
    }
    /* (non-Javadoc)
     * @see org.geoserver.security.password.KeystoreProvider#getKeyStoreProvderFile()
     */
    @Override
    public File getFile() {
        return getResource().file();
    }
    /* (non-Javadoc)
     * @see org.geoserver.security.password.KeystoreProvider#reloadKeyStore()
     */
    @Override
    public void reloadKeyStore() throws IOException{
        ks=null;
        assertActivatedKeyStore();
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.password.KeystoreProvider#getKey(java.lang.String)
     */
    @Override
    public Key getKey(String alias) throws IOException{
        assertActivatedKeyStore();
        try {
            char[] passwd = securityManager.getMasterPassword();
            try {
                return ks.getKey(alias, passwd);
            }
            finally {
                securityManager.disposePassword(passwd);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.geoserver.security.password.KeystoreProvider#getConfigPasswordKey()
     */
    @Override
    public byte[] getConfigPasswordKey() throws IOException{
        SecretKey key = getSecretKey(CONFIGPASSWORDKEY);
        if (key==null) return null;
        return key.getEncoded();
    }
    
    /* (non-Javadoc)
     * @see org.geoserver.security.password.KeystoreProvider#hasConfigPasswordKey()
     */
    @Override
    public boolean hasConfigPasswordKey() throws IOException {
        return containsAlias(CONFIGPASSWORDKEY);
    }
    
        
    /* (non-Javadoc)
     * @see org.geoserver.security.password.KeystoreProvider#containsAlias(java.lang.String)
     */
    @Override
    public boolean containsAlias(String alias) throws IOException{
        assertActivatedKeyStore();
        try {
            return ks.containsAlias(alias);
        } catch (KeyStoreException e) {
            throw new IOException(e);
        }
    }
    /* (non-Javadoc)
     * @see org.geoserver.security.password.KeystoreProvider#getUserGRoupKey(java.lang.String)
     */
    @Override
    public byte[] getUserGroupKey(String serviceName) throws IOException{
        SecretKey key = getSecretKey(aliasForGroupService(serviceName));
        if (key==null) return null;
        return key.getEncoded();

    }
    
    /* (non-Javadoc)
     * @see org.geoserver.security.password.KeystoreProvider#hasUserGRoupKey(java.lang.String)
     */
    @Override
    public boolean hasUserGroupKey(String serviceName) throws IOException {
        return containsAlias(aliasForGroupService(serviceName));
        
    }

    
    /* (non-Javadoc)
     * @see org.geoserver.security.password.KeystoreProvider#getSecretKey(java.lang.String)
     */
    @Override
    public SecretKey getSecretKey(String name) throws IOException{
        Key key = getKey(name);
        if (key==null) return null;
        if ((key instanceof SecretKey) == false)
            throw new IOException("Invalid key type for: "+name);
        return (SecretKey) key;
    }
    
    /* (non-Javadoc)
     * @see org.geoserver.security.password.KeystoreProvider#getPublicKey(java.lang.String)
     */
    @Override
    public PublicKey getPublicKey(String name) throws IOException{
        Key key = getKey(name);
        if (key==null) return null;
        if ((key instanceof PublicKey) == false)
            throw new IOException("Invalid key type for: "+name);
        return (PublicKey) key;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.password.KeystoreProvider#getPrivateKey(java.lang.String)
     */
    @Override
    public PrivateKey getPrivateKey(String name) throws IOException{
        Key key = getKey(name);
        if (key==null) return null;
        if ((key instanceof PrivateKey) == false)
            throw new IOException("Invalid key type for: "+name);
        return (PrivateKey) key;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.password.KeystoreProvider#aliasForGroupService(java.lang.String)
     */
    @Override
    public String aliasForGroupService(String serviceName) {
        StringBuffer buff = new StringBuffer(USERGROUP_PREFIX);
        buff.append(serviceName);
        buff.append(USERGROUP_POSTFIX);
        return buff.toString();            
    }
    
    /**
     * Opens or creates a {@link KeyStore} using the file
     * {@link #DEFAULT_FILE_NAME}
     * 
     * Throws an exception for an invalid master key
     * 
     * @throws IOException 
     */            
    protected void assertActivatedKeyStore() throws IOException {
        if (ks != null) 
            return;
        
        char[] passwd = securityManager.getMasterPassword();
        try {
            ks = KeyStore.getInstance(KEYSTORETYPE);
            if (getResource().getType()==Type.UNDEFINED) { // create an empy one
                ks.load(null, passwd);
                addInitialKeys();
                try(OutputStream os = getResource().out()) {
                    ks.store(os, passwd);
                }
            } else {
                try(InputStream is = getResource().in()) {
                    ks.load(is, passwd);
                }
            }
        } catch (Exception ex) {
            if (ex instanceof IOException) // avoid useless wrapping
                throw (IOException) ex;
            throw new IOException (ex);
        }
        finally {
            securityManager.disposePassword(passwd);
        }
    }
    
    /* (non-Javadoc)
     * @see org.geoserver.security.password.KeystoreProvider#isKeystorePassword(java.lang.String)
     */
    @Override
    public boolean isKeyStorePassword(char[] password) throws IOException{
        if (password==null) return false;
        assertActivatedKeyStore();
        
        KeyStore testStore=null;
        try {
            testStore = KeyStore.getInstance(KEYSTORETYPE);
        } catch (KeyStoreException e1) {
            // should not happen, see assertActivatedKeyStore
            throw new RuntimeException(e1);
        }
        FileInputStream fis =
                new FileInputStream(getFile());
        try {
            testStore.load(fis, password);
        } catch (IOException e2) {
            // indicates invalid password
            return false;
        } catch (Exception e) {
            // should not happen, see assertActivatedKeyStore
            throw new RuntimeException(e);
        }                
        fis.close();     
        return true;
    }
    
    /* (non-Javadoc)
     * @see org.geoserver.security.password.KeystoreProvider#setSecretKey(java.lang.String, java.lang.String)
     */
    @Override
    public void setSecretKey(String alias, char[] key) throws IOException {
        assertActivatedKeyStore();
        SecretKey mySecretKey=new SecretKeySpec(toBytes(key),"PBE");
        KeyStore.SecretKeyEntry skEntry =
            new KeyStore.SecretKeyEntry(mySecretKey);
        char[] passwd = securityManager.getMasterPassword();
        try {
            ks.setEntry(alias, skEntry, new KeyStore.PasswordProtection(passwd));
        } catch (KeyStoreException e) {
            throw new IOException(e);
        }
        finally {
            securityManager.disposePassword(passwd);
        }
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.password.KeystoreProvider#setUserGroupKey(java.lang.String, java.lang.String)
     */
    @Override
    public void setUserGroupKey(String serviceName,char[] password) throws IOException{
        String alias = aliasForGroupService(serviceName);
        setSecretKey(alias, password);
    }
    
    /* (non-Javadoc)
     * @see org.geoserver.security.password.KeystoreProvider#removeKey(java.lang.String)
     */
    @Override
    public void removeKey(String alias ) throws IOException {
        assertActivatedKeyStore();
        try {
            ks.deleteEntry(alias);
        } catch (KeyStoreException e) {
            throw new IOException(e);
        }
    }

    
    /* (non-Javadoc)
     * @see org.geoserver.security.password.KeystoreProvider#storeKeyStore()
     */
    @Override
    public void storeKeyStore() throws IOException{
        // store away the keystore
        assertActivatedKeyStore();
        FileOutputStream fos = new  FileOutputStream(getFile());

        char[] passwd = securityManager.getMasterPassword(); 
        try {
            ks.store(fos, passwd);
        } catch (Exception e) {
            throw new IOException(e);
        }
        finally {
            securityManager.disposePassword(passwd);
        }
        fos.close();
    }
    
    /**
     * Creates initial key entries
     * auto generated keys
     * {@link #CONFIGPASSWORDKEY}
     * 
     * @throws IOException
     */
    protected void addInitialKeys() throws IOException {
        //TODO:scramble
        RandomPasswordProvider randPasswdProvider = 
                getSecurityManager().getRandomPassworddProvider(); 
        
        char[] configKey = randPasswdProvider.getRandomPasswordWithDefaultLength();
        setSecretKey( CONFIGPASSWORDKEY, configKey);
    }
    
    /* (non-Javadoc)
     * @see org.geoserver.security.password.KeystoreProvider#prepareForMasterPasswordChange(java.lang.String, java.lang.String)
     */
    @Override
    public void prepareForMasterPasswordChange(char[] oldPassword, char[] newPassword) throws IOException{

                
        Resource dir = getResource().parent();
        Resource newKSFile = dir.get(PREPARED_FILE_NAME);
        if (newKSFile.getType()!=Type.UNDEFINED)
            newKSFile.delete();
        
        try {
            KeyStore oldKS=KeyStore.getInstance(KEYSTORETYPE);
            FileInputStream fin = new FileInputStream(getFile());
            oldKS.load(fin, oldPassword);
            fin.close();
            
            KeyStore newKS = KeyStore.getInstance(KEYSTORETYPE);
            newKS.load(null, newPassword);
            KeyStore.PasswordProtection protectionparam = 
                    new KeyStore.PasswordProtection(newPassword);

            Enumeration<String> enumeration = oldKS.aliases();
            while (enumeration.hasMoreElements()) {
                String alias =enumeration.nextElement();
                Key key = oldKS.getKey(alias, oldPassword);
                KeyStore.Entry entry =null;
                if (key instanceof SecretKey) 
                    entry = new KeyStore.SecretKeyEntry((SecretKey)key);
                if (key instanceof PrivateKey) 
                    entry = new KeyStore.PrivateKeyEntry((PrivateKey)key,
                            oldKS.getCertificateChain(alias));                         
                if (key instanceof PublicKey) 
                    entry = new KeyStore.TrustedCertificateEntry(oldKS.getCertificate(alias));                         
                if (entry == null)
                    LOGGER.warning("Unknown key in store, alias: "+alias+
                            " class: "+ key.getClass().getName());
                else
                    newKS.setEntry(alias, entry, protectionparam);
            }            
           
            try(OutputStream os = newKSFile.out();){
                newKS.store(os, newPassword);
            }

        } catch (Exception ex) {
            throw new IOException(ex);
        }

    }

    /* (non-Javadoc)
     * @see org.geoserver.security.password.KeystoreProvider#abortMasterPasswordChange()
     */
    @Override
    public void abortMasterPasswordChange() {
        Resource dir = getResource().parent();
        Resource newKSFile = dir.get(PREPARED_FILE_NAME);
        if (newKSFile.getType()!=Type.UNDEFINED) {
            //newKSFile.delete();
        }
        
    }
    
    
    /* (non-Javadoc)
     * @see org.geoserver.security.password.KeystoreProvider#commitMasterPasswordChange()
     */
    @Override
    public void commitMasterPasswordChange() throws IOException {
        Resource dir = getResource().parent();
        Resource newKSFile = dir.get(PREPARED_FILE_NAME);
        Resource oldKSFile = dir.get(DEFAULT_FILE_NAME);
        
        if (newKSFile.getType()==Type.UNDEFINED)
            return; //nothing to do

        if (oldKSFile.getType()==Type.UNDEFINED)
            return; //not initialized
        
        // Try to open with new password
        try(InputStream in = newKSFile.in()) {
            char[] passwd = securityManager.getMasterPassword();
            
            try {
                KeyStore newKS = KeyStore.getInstance(KEYSTORETYPE);
                newKS.load(in, passwd);
                
                // to be sure, decrypt all keys
                Enumeration<String> enumeration = newKS.aliases();
                while (enumeration.hasMoreElements()) {
                    newKS.getKey(enumeration.nextElement(), passwd);
                }            
                in.close();
                if (oldKSFile.delete()==false) { 
                    LOGGER.severe("cannot delete " +getFile().getCanonicalPath());
                    return;
                }
                
                if (newKSFile.renameTo(oldKSFile)==false) {
                    String msg = "cannot rename "+ newKSFile.path();
                    msg += "to " + oldKSFile.path();
                    msg += "Try to rename manually and restart";
                    LOGGER.severe(msg);
                    return;
                }
                reloadKeyStore();
                LOGGER.info("Successfully changed master password");            
            } catch (IOException e) {
                String msg = "Error creating new keystore: " + newKSFile.path();
                LOGGER.log(Level.WARNING, msg, e);
                throw e;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
    
            finally {
                securityManager.disposePassword(passwd);
            }
        }
    }
}
