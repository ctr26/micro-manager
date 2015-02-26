package org.micromanager.internal.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import org.micromanager.PropertyMap;
import org.micromanager.UserProfile;
import org.micromanager.data.internal.DefaultPropertyMap;
import org.micromanager.internal.utils.MDUtils;

public class DefaultUserProfile implements UserProfile {
   private static final String USERNAME_MAPPING_FILE = "Profiles.txt";
   private static final String GLOBAL_USER = "Global defaults";
   public static final String DEFAULT_USER = "Default user";
   private static final String ALWAYS_USE_DEFAULT_USER = "always use the default user profile";

   private static DefaultUserProfile staticInstance_;
   static {
      staticInstance_ = new DefaultUserProfile();
   }
   private HashMap<String, String> nameToFile_;
   private String userName_;
   private DefaultPropertyMap globalProfile_;
   private DefaultPropertyMap userProfile_;

   public DefaultUserProfile() {
      nameToFile_ = loadUserMapping();
      globalProfile_ = loadUser(GLOBAL_USER);
      // Naturally we start with the default user loaded.
      setCurrentUser(DEFAULT_USER);
   }

   /**
    * Read the username mapping file so we know which user's profile is in
    * which file.
    */
   private HashMap<String, String> loadUserMapping() {
      JSONObject mapping = new JSONObject();
      HashMap<String, String> result = new HashMap<String, String>();
      File tmp = new File(JavaUtils.getApplicationDataPath() +
            "/" + USERNAME_MAPPING_FILE);
      if (!tmp.exists()) {
         ReportingUtils.logMessage("Creating user profile mapping file");
         // No file to be found; create it with the default user.
         result.put(GLOBAL_USER, UserProfile.GLOBAL_SETTINGS_FILE);
         writeUserMapping(result);
      }
      try {
         mapping = new JSONObject(
               loadFileToString(JavaUtils.getApplicationDataPath() +
                  "/" + USERNAME_MAPPING_FILE));
      }
      catch (JSONException e) {
         ReportingUtils.logError(e, "Unable to load user profiles mapping file");
      }
      for (String key : MDUtils.getKeys(mapping)) {
         try {
            result.put(key, mapping.getString(key));
         }
         catch (JSONException e) {
            ReportingUtils.logError(e, "Unable to get filename for user " + key);
         }
      }
      return result;
   }

   /**
    * Write out the current mapping of usernames to profile files.
    */
   private void writeUserMapping(HashMap<String, String> nameToFile) {
      JSONObject mapping = new JSONObject();
      for (String name : nameToFile.keySet()) {
         try {
            mapping.put(name, nameToFile.get(name));
         }
         catch (JSONException e) {
            ReportingUtils.logError(e, "Unable to add entry for profile " + name + " to file " + nameToFile.get(name));
         }
      }

      JavaUtils.createApplicationDataPathIfNeeded();
      try {
         FileWriter writer = new FileWriter(
               JavaUtils.getApplicationDataPath() + "/" + USERNAME_MAPPING_FILE);
         writer.write(mapping.toString(2) + "\n");
         writer.close();
      }
      catch (FileNotFoundException e) {
         ReportingUtils.logError(e, "Unable to open writer to save user profile mapping file");
      }
      catch (JSONException e) {
         ReportingUtils.logError(e, "Unable to convert JSON mapping into string");
      }
      catch (IOException e) {
         ReportingUtils.logError(e, "Exception when writing user profile mapping file");
      }
   }

   /**
    * Load a PropertyMap for a given user; return an empty PropertyMap if that
    * user doesn't yet exist, creating the profile for them in the process.
    */
   private DefaultPropertyMap loadUser(String userName) {
      if (!nameToFile_.containsKey(userName)) {
         // Create a new profile.
         ReportingUtils.logMessage("User name " + userName + " not found in profile mapping; creating that user");
         addUser(userName);
         return (DefaultPropertyMap) (new DefaultPropertyMap.Builder().build());
      }
      String filename = nameToFile_.get(userName);
      JavaUtils.createApplicationDataPathIfNeeded();
      String path = JavaUtils.getApplicationDataPath() + "/" + filename;
      return loadPropertyMap(path);
   }

   private DefaultPropertyMap loadPropertyMap(String path) {
      String contents = loadFileToString(path);
      if (contents == null) {
         // That file doesn't exist yet; create a new empty user.
         ReportingUtils.logMessage("Asked to load file at path " + path +
               " which doesn't exist; instead providing a default empty PropertyMap.");
         return (DefaultPropertyMap) (new DefaultPropertyMap.Builder().build());
      }
      try {
         return DefaultPropertyMap.fromJSON(
               new JSONObject(loadFileToString(path)));
      }
      catch (JSONException e) {
         ReportingUtils.logError(e, "Failed to convert file contents for file at " + path + " to JSON");
         return null;
      }
   }

   /**
    * Load the contents of a file and return them as a string. Adapted from
    * http://stackoverflow.com/questions/326390/how-to-create-a-java-string-from-the-contents-of-a-file
    * TODO: should probably be moved into utils somewhere.
    */
   private String loadFileToString(String path) {
      File propFile = new File(path);
      if (!propFile.exists()) {
         return null;
      }
      StringBuilder contents = new StringBuilder((int) propFile.length());
      Scanner scanner;
      try {
         scanner = new Scanner(propFile);
      }
      catch (FileNotFoundException e) {
         // This should never happen since we checked if file exists.
         ReportingUtils.logError(e,
               "Somehow failed to open scanner for file at " + path);
         return null;
      }
      String separator = System.getProperty("line.separator");
      try {
         while (scanner.hasNextLine()) {
            contents.append(scanner.nextLine() + separator);
         }
      }
      finally {
         scanner.close();
      }
      return contents.toString();
   }

   /**
    * Append a class name to the provided key to generate a unique-across-MM
    * key.
    * Note that saveProfileSubsetToFile() and clearProfileSubset() assume that
    * keys start with the class' canonical name.
    */
   private String genKey(Class<?> c, String key) {
      return c.getCanonicalName() + ":" + key;
   }

   // Getters/setters. There is a ton of duplication here; unfortunately, I
   // don't see any real way to avoid it. Here's hoping that my auto-generated
   // code doesn't have any typos in it!

   @Override
   public String getString(Class<?> c, String key, String fallback) {
      key = genKey(c, key);
      synchronized(userProfile_) {
         String result = userProfile_.getString(key);
         if (result != null) {
            return result;
         }
      }
      // Try the global profile.
      synchronized(globalProfile_) {
         String result = globalProfile_.getString(key);
         if (result != null) {
            return result;
         }
      }
      // Give up.
      return fallback;
   }
   @Override
   public String[] getStringArray(Class<?> c, String key, String[] fallback) {
      key = genKey(c, key);
      synchronized(userProfile_) {
         String[] result = userProfile_.getStringArray(key);
         if (result != null) {
            return result;
         }
      }
      // Try the global profile.
      synchronized(globalProfile_) {
         String[] result = globalProfile_.getStringArray(key);
         if (result != null) {
            return result;
         }
      }
      // Give up.
      return fallback;
   }
   @Override
   public void setString(Class<?> c, String key, String value) {
      synchronized(userProfile_) {
         userProfile_ = (DefaultPropertyMap) userProfile_.copy().putString(genKey(c, key), value).build();
      }
   }
   @Override
   public void setStringArray(Class<?> c, String key, String[] value) {
      synchronized(userProfile_) {
         userProfile_ = (DefaultPropertyMap) userProfile_.copy().putStringArray(genKey(c, key), value).build();
      }
   }

   @Override
   public Integer getInt(Class<?> c, String key, Integer fallback) {
      key = genKey(c, key);
      synchronized(userProfile_) {
         Integer result = userProfile_.getInt(key);
         if (result != null) {
            return result;
         }
      }
      // Try the global profile.
      synchronized(globalProfile_) {
         Integer result = globalProfile_.getInt(key);
         if (result != null) {
            return result;
         }
      }
      // Give up.
      return fallback;
   }
   @Override
   public Integer[] getIntArray(Class<?> c, String key, Integer[] fallback) {
      key = genKey(c, key);
      synchronized(userProfile_) {
         Integer[] result = userProfile_.getIntArray(key);
         if (result != null) {
            return result;
         }
      }
      // Try the global profile.
      synchronized(globalProfile_) {
         Integer[] result = globalProfile_.getIntArray(key);
         if (result != null) {
            return result;
         }
      }
      // Give up.
      return fallback;
   }
   @Override
   public void setInt(Class<?> c, String key, Integer value) {
      synchronized(userProfile_) {
         userProfile_ = (DefaultPropertyMap) userProfile_.copy().putInt(genKey(c, key), value).build();
      }
   }
   @Override
   public void setIntArray(Class<?> c, String key, Integer[] value) {
      synchronized(userProfile_) {
         userProfile_ = (DefaultPropertyMap) userProfile_.copy().putIntArray(genKey(c, key), value).build();
      }
   }

   @Override
   public Double getDouble(Class<?> c, String key, Double fallback) {
      key = genKey(c, key);
      synchronized(userProfile_) {
         Double result = userProfile_.getDouble(key);
         if (result != null) {
            return result;
         }
      }
      // Try the global profile.
      synchronized(globalProfile_) {
         Double result = globalProfile_.getDouble(key);
         if (result != null) {
            return result;
         }
      }
      // Give up.
      return fallback;
   }
   @Override
   public Double[] getDoubleArray(Class<?> c, String key, Double[] fallback) {
      key = genKey(c, key);
      synchronized(userProfile_) {
         Double[] result = userProfile_.getDoubleArray(key);
         if (result != null) {
            return result;
         }
      }
      // Try the global profile.
      synchronized(globalProfile_) {
         Double[] result = globalProfile_.getDoubleArray(key);
         if (result != null) {
            return result;
         }
      }
      // Give up.
      return fallback;
   }
   @Override
   public void setDouble(Class<?> c, String key, Double value) {
      synchronized(userProfile_) {
         userProfile_ = (DefaultPropertyMap) userProfile_.copy().putDouble(genKey(c, key), value).build();
      }
   }
   @Override
   public void setDoubleArray(Class<?> c, String key, Double[] value) {
      synchronized(userProfile_) {
         userProfile_ = (DefaultPropertyMap) userProfile_.copy().putDoubleArray(genKey(c, key), value).build();
      }
   }

   @Override
   public Boolean getBoolean(Class<?> c, String key, Boolean fallback) {
      key = genKey(c, key);
      synchronized(userProfile_) {
         Boolean result = userProfile_.getBoolean(key);
         if (result != null) {
            return result;
         }
      }
      // Try the global profile.
      synchronized(globalProfile_) {
         Boolean result = globalProfile_.getBoolean(key);
         if (result != null) {
            return result;
         }
      }
      // Give up.
      return fallback;
   }
   @Override
   public Boolean[] getBooleanArray(Class<?> c, String key, Boolean[] fallback) {
      key = genKey(c, key);
      synchronized(userProfile_) {
         Boolean[] result = userProfile_.getBooleanArray(key);
         if (result != null) {
            return result;
         }
      }
      // Try the global profile.
      synchronized(globalProfile_) {
         Boolean[] result = globalProfile_.getBooleanArray(key);
         if (result != null) {
            return result;
         }
      }
      // Give up.
      return fallback;
   }
   @Override
   public void setBoolean(Class<?> c, String key, Boolean value) {
      synchronized(userProfile_) {
         userProfile_ = (DefaultPropertyMap) userProfile_.copy().putBoolean(genKey(c, key), value).build();
      }
   }
   @Override
   public void setBooleanArray(Class<?> c, String key, Boolean[] value) {
      synchronized(userProfile_) {
         userProfile_ = (DefaultPropertyMap) userProfile_.copy().putBooleanArray(genKey(c, key), value).build();
      }
   }

   @Override
   public void saveProfileSubsetToFile(Class<?> c, String path) throws IOException {
      // Make a copy profile, and save that.
      DefaultPropertyMap.Builder builder = new DefaultPropertyMap.Builder();
      // NOTE: this should match genKey()
      String className = c.getCanonicalName();
      synchronized(userProfile_) {
         for (String key : userProfile_.getKeys()) {
            if (key.startsWith(className)) {
               builder.putProperty(key, userProfile_.getProperty(key));
            }
         }
      }
      savePropertyMapToFile((DefaultPropertyMap) builder.build(), path);
   }

   @Override
   public void saveProfile() throws IOException {
      JavaUtils.createApplicationDataPathIfNeeded();
      saveProfileToFile(JavaUtils.getApplicationDataPath() +
            "/" + nameToFile_.get(userName_));
   }

   @Override
   public void saveProfileToFile(String path) throws IOException {
      savePropertyMapToFile(userProfile_, path);
   }

   private void savePropertyMapToFile(DefaultPropertyMap properties, String path) throws IOException {
      JSONObject serialization;
      synchronized(properties) {
         serialization = properties.toJSON();
      }
      try {
         FileWriter writer = new FileWriter(path);
         writer.write(serialization.toString(2) + "\n");
         writer.close();
      }
      catch (FileNotFoundException e) {
         ReportingUtils.logError(e, "Unable to open writer to save user profile mapping file");
      }
      catch (JSONException e) {
         ReportingUtils.logError(e, "Unable to convert JSON mapping into string");
      }
   }

   @Override
   public void appendFile(String path) {
      File file = new File(path);
      if (!file.exists()) {
         ReportingUtils.logError("Asked to load file at " + path + " that does not exist.");
         return;
      }
      PropertyMap properties = loadPropertyMap(path);
      userProfile_ = (DefaultPropertyMap) userProfile_.merge(properties);
   }

   public Set<String> getUserNames() {
      // HACK: don't reveal the global profile since it's not technically a
      // "user".
      HashSet<String> result = new HashSet<String>(nameToFile_.keySet());
      result.remove(GLOBAL_USER);
      return result;
   }

   public void addUser(String userName) {
      // Assign a filename for the user, which should be 1 greater than the
      // largest current numerical filename we're using.
      Pattern pattern = Pattern.compile("profile-(\\d+).txt");
      int fileIndex = 0;
      for (String key : nameToFile_.keySet()) {
         String fileName = nameToFile_.get(key);
         Matcher matcher = pattern.matcher(fileName);
         if (!matcher.matches()) {
            continue;
         }
         try {
            int index = Integer.parseInt(matcher.group(1));
            fileIndex = Math.max(index, fileIndex);
         }
         catch (NumberFormatException e) {
            // No number to use.
            continue;
         }
      }
      String newFile = "profile-" + (fileIndex + 1) + ".txt";
      nameToFile_.put(userName, newFile);
      writeUserMapping(nameToFile_);
   }

   public void setCurrentUser(String userName) {
      userName_ = userName;
      userProfile_ = loadUser(userName);
   }

   @Override
   public void clearProfileSubset(Class<?> c) {
      // Make a copy of the map that contains everything except the keys for
      // the specified class.
      synchronized(userProfile_) {
         DefaultPropertyMap.Builder builder = new DefaultPropertyMap.Builder();
         for (String key : userProfile_.getKeys()) {
            if (!key.startsWith(c.getCanonicalName())) {
               builder.putProperty(key, userProfile_.getProperty(key));
            }
         }
         userProfile_ = (DefaultPropertyMap) builder.build();
      }
   }

   /**
    * Delete all parameters for the current user.
    */
   public void clearProfile() {
      userProfile_ = (DefaultPropertyMap) (new DefaultPropertyMap.Builder().build());
   }

   public static DefaultUserProfile getInstance() {
      return staticInstance_;
   }

   /**
    * This special property controls whether or not we always use the
    * "Default user" profile.
    */
   public static boolean getShouldAlwaysUseDefaultProfile() {
      DefaultUserProfile profile = getInstance();
      // This parameter is stored for the default user. Just in case we *are*
      // using the default user, wrap this in synchronized.
      synchronized(profile.userProfile_) {
         PropertyMap defaultUser = profile.loadUser(DEFAULT_USER);
         Boolean result = defaultUser.getBoolean(
               profile.genKey(DefaultUserProfile.class, ALWAYS_USE_DEFAULT_USER));
         if (result == null) {
            return false;
         }
         return result;
      }
   }

   /**
    * As above, but set the value. It will only take effect after a restart.
    */
   public static void setShouldAlwaysUseDefaultProfile(boolean shouldUseDefault) {
      DefaultUserProfile profile = getInstance();
      // Again, in case we're using the default user already, we wrap this
      // in synchronized.
      synchronized(profile.userProfile_) {
         // In order to simplify saving things (since saveProfile() always
         // saves the current user), we temporarily switch to the
         // default user for this operation.
         String curUser = profile.userName_;
         profile.setCurrentUser(DEFAULT_USER);
         profile.setBoolean(DefaultUserProfile.class,
               ALWAYS_USE_DEFAULT_USER, shouldUseDefault);
         try {
            profile.saveProfile();
         }
         catch (IOException e) {
            ReportingUtils.logError(e, "Unable to save whether or not to always use the default profile");
         }
         profile.setCurrentUser(curUser);
      }
   }
}
