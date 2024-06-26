
package tolt.server.database;

import java.util.Vector;
import java.lang.Math;

import tolt.server.service.logging.Logging;
import tolt.server.security.util.SHAWrapper;
import tolt.server.core.module.Message;
import tolt.server.database.userbase.*;
import tolt.server.database.channelbase.*;

public class Database {

    public static Object mutex = new Object();

    private static int counter = 0;
    public static void tick () {

        counter++;
        if (counter == 10) { counter = 0;

            User.tick();
            Channel.tick();
        }
    }
    public static void saveCache () {

        Channel.finalTick();
    }

    public static class User {

        private static Vector<UserEntry> entryCache = new Vector<UserEntry>();
        private static Vector<Long> accessStamps = new Vector<Long>();

        public static UserEntry get (String userHash) { synchronized (Database.mutex) {
            for (int i = 0; i < entryCache.size(); ++i)
                if (entryCache.get(i).userHash.equals(userHash)) {
                    accessStamps.set(i, System.currentTimeMillis() / 1000L);
                    return entryCache.get(i);
                }
            return load(userHash);
        } }
        public static void set (UserEntry newEntry) { synchronized (Database.mutex) {
            for (int i = 0; i < entryCache.size(); ++i)
                if (entryCache.get(i).userHash.equals(newEntry.userHash)) {
                    accessStamps.set(i, System.currentTimeMillis() / 1000L);
                    entryCache.set(i, newEntry);
                    save(newEntry.userHash);
                }
        } }
        private static UserEntry internalGet (String userHash) {
            for (int i = 0; i < entryCache.size(); ++i)
                if (entryCache.get(i).userHash.equals(userHash)) {
                    accessStamps.set(i, System.currentTimeMillis() / 1000L);
                    return entryCache.get(i);
                }
            return load(userHash);
        }
        private static void internalSet (UserEntry newEntry) {
            for (int i = 0; i < entryCache.size(); ++i)
                if (entryCache.get(i).userHash.equals(newEntry.userHash)) {
                    accessStamps.set(i, System.currentTimeMillis() / 1000L);
                    entryCache.set(i, newEntry);
                    save(newEntry.userHash);
                }
        }

        public static void tick () { synchronized (Database.mutex) {
            cacheCheck();
        } }

        private static UserEntry load (String userHash) {
            UserEntry entry = Userbase.loadUser(userHash);
            accessStamps.add(System.currentTimeMillis() / 1000L);
            entryCache.add(entry);
            Logging.debug("loaded " + userHash);
            return entry;
        }
        private static void save (String userHash) {
            for (var entry : entryCache) if (entry.userHash.equals(userHash))
                Userbase.saveUser(entry);
        }
        private static void save () {
            for (var entry : entryCache) Userbase.saveUser(entry);
        }
        private static void cacheCheck () {
            Vector<String> removeList = new Vector<String>();
            for (int i = 0; i < accessStamps.size(); ++i)
                if ((System.currentTimeMillis() / 1000L) - accessStamps.get(i) > 60)
                    removeList.add(entryCache.get(i).userHash);
            for (String hash : removeList)
                for (int i = 0; i < entryCache.size(); ++i)
                    if (entryCache.get(i).userHash.equals(hash)) {
                        accessStamps.remove(i); entryCache.remove(i);
                        Logging.debug("unloaded " + hash);
                    }
        }

        public static int register (
            String username,
            String passwordHash,
            String realName,
            String emailAddress,
            String requesterIPA
        ) { synchronized (Database.mutex) {
            return Userbase.tryCreateUser(
                username, passwordHash, realName, emailAddress, requesterIPA
            );
        } }
        public static int login (
            String username,
            String passwordHash,
            String requesterIPA
        ) { synchronized (Database.mutex) {
            if (!Userbase.userExists(SHAWrapper.sha256Text(username))) return -1;
            UserEntry user = internalGet(SHAWrapper.sha256Text(username)); if (user == null) return -2;
            if (!user.passwordHash.equals(passwordHash)) return -3;
            user.loginCount++;
            user.lastLoginTimeStamp = System.currentTimeMillis() / 1000L;
            user.lastLoginIPA = requesterIPA;
            internalSet(user);
            return 0;
        } }
        public static String[] getAllUserHashes () { synchronized (Database.mutex) {
            return Userbase.getAllUserHashes();
        } }
    }

    public static class Channel {

        private static Vector<ChannelEntry> entryCache = new Vector<ChannelEntry>();
        private static Vector<Long> accessStamps = new Vector<Long>();

        public static ChannelEntry get (String channelHash) { synchronized (Database.mutex) {
            for (int i = 0; i < entryCache.size(); ++i)
                if (entryCache.get(i).channelHash.equals(channelHash)) {
                    accessStamps.set(i, System.currentTimeMillis() / 1000L);
                    return entryCache.get(i);
                }
            return load(channelHash);
        } }
        public static void set (ChannelEntry newEntry) { synchronized (Database.mutex) {
            for (int i = 0; i < entryCache.size(); ++i)
                if (entryCache.get(i).channelHash.equals(newEntry.channelHash)) {
                    accessStamps.set(i, System.currentTimeMillis() / 1000L);
                    entryCache.set(i, newEntry);
                    entryCache.get(i).modified = true;
                }
        } }
        private static ChannelEntry internalGet (String channelHash) {
            for (int i = 0; i < entryCache.size(); ++i)
                if (entryCache.get(i).channelHash.equals(channelHash)) {
                    accessStamps.set(i, System.currentTimeMillis() / 1000L);
                    return entryCache.get(i);
                }
            return load(channelHash);
        }
        private static void internalSet (ChannelEntry newEntry) {
            for (int i = 0; i < entryCache.size(); ++i)
                if (entryCache.get(i).channelHash.equals(newEntry.channelHash)) {
                    accessStamps.set(i, System.currentTimeMillis() / 1000L);
                    entryCache.set(i, newEntry);
                    entryCache.get(i).modified = true;
                }
        }

        public static void tick () { synchronized (Database.mutex) {
            cacheCheck();
        } }
        public static void finalTick () { synchronized (Database.mutex) {
            saveCheck();
        } }

        private static ChannelEntry load (String channelHash) {
            ChannelEntry entry = Channelbase.loadChannel(channelHash);
            accessStamps.add(System.currentTimeMillis() / 1000L);
            entry.load();
            entryCache.add(entry);
            Logging.debug("loaded " + channelHash);
            return entry;
        }
        private static void save (String channelHash) {
            for (var entry : entryCache) if (entry.channelHash.equals(channelHash)) {
                entry.save();
                Channelbase.saveChannel(entry);
            }
        }
        private static void cacheCheck () {
            Vector<String> removeList = new Vector<String>();
            for (int i = 0; i < accessStamps.size(); ++i)
                if ((System.currentTimeMillis() / 1000L) - accessStamps.get(i) > 60)
                    removeList.add(entryCache.get(i).channelHash);
            for (String hash : removeList)
                for (int i = 0; i < entryCache.size(); ++i)
                    if (entryCache.get(i).channelHash.equals(hash)) {
                        save(entryCache.get(i).channelHash); accessStamps.remove(i); entryCache.remove(i);
                        Logging.debug("saved and unloaded " + hash);
                    }
        }
        private static void saveCheck () {
            for (int i = 0; i < entryCache.size(); ++i) {
                if (entryCache.get(i).modified) save(entryCache.get(i).channelHash);
                Logging.debug("saved " + entryCache.get(i).channelHash);
            }
        }

        public static int create (
            String channelName,
            String channelNameContext,
            String creationUsername
        ) { synchronized (Database.mutex) {
            return Channelbase.tryCreateChannel(
                channelName, channelNameContext, creationUsername
            );
        } }

        public static void addMessage (String channelHash, Message message) { synchronized (Database.mutex) {
            ChannelEntry entry = internalGet(channelHash);
            entry.messageCache.add(message);
            entry.modified = true;
            internalSet(entry);
        } }
        public static Message[] getMessages (String channelHash, int count) { synchronized (Database.mutex) {
            ChannelEntry entry = internalGet(channelHash);
            int returnCount = Math.min(entry.messageCache.size(), count);
            Message[] returnArray = new Message[returnCount];
            for (int i = 0; i < returnCount; ++i)
                returnArray[i] = entry.messageCache.get(i);
            return returnArray;
        } }
    }
}
