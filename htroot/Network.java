// Network.java
// -----------------------
// part of YaCy
// (C) by Michael Peter Christen; mc@anomic.de
// first published on http://www.anomic.de
// Frankfurt, Germany, 2004, 2005
// last major change: 16.02.2005
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// Using this software in any meaning (reading, learning, copying, compiling,
// running) means that you agree that the Author(s) is (are) not responsible
// for cost, loss of data or any harm that may be caused directly or indirectly
// by usage of this softare or this documentation. The usage of this software
// is on your own risk. The installation and usage (starting/running) of this
// software may allow other people or application to access your computer and
// any attached devices and is highly dependent on the configuration of the
// software which must be done by the user of the software; the author(s) is
// (are) also not responsible for proper configuration and usage of the
// software, even if provoked by documentation provided together with
// the software.
//
// Any changes to this file according to the GPL as documented in the file
// gpl.txt aside this file in the shipment you received can be done to the
// lines that follows this copyright notice here, but changes must not be
// done inside the copyright notive above. A re-distribution must contain
// the intact and unchanged copyright notice.
// Contributions and changes to the program code must be marked as such.

// You must compile this file with
// javac -classpath .:../classes Network.java
// if the shell's current path is HTROOT

import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.io.IOException;

import de.anomic.http.httpHeader;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;
import de.anomic.server.serverDate;
import de.anomic.yacy.yacyClient;
import de.anomic.yacy.yacyCore;
import de.anomic.yacy.yacySeed;
import de.anomic.yacy.yacyNewsRecord;
import de.anomic.yacy.yacyNewsPool;

public class Network {
    
    public static serverObjects respond(httpHeader header, serverObjects post, serverSwitch sb) {
        // return variable that accumulates replacements
        serverObjects prop = new serverObjects();
        boolean overview = (post == null) || (((String) post.get("page", "0")).equals("0"));
        
        String mySeedType = yacyCore.seedDB.mySeed.get("PeerType", "virgin");
        boolean iAmActive = (mySeedType.equals("senior")) || (mySeedType.equals("principal"));
        
        if (overview) {
            long accActLinks = yacyCore.seedDB.countActiveURL();
            long accActWords = yacyCore.seedDB.countActiveRWI();
            long accPassLinks = yacyCore.seedDB.countPassiveURL();
            long accPassWords = yacyCore.seedDB.countPassiveRWI();
            long accPotLinks = yacyCore.seedDB.countPotentialURL();
            long accPotWords = yacyCore.seedDB.countPotentialRWI();
            
            int conCount = yacyCore.seedDB.sizeConnected();
            int disconCount = yacyCore.seedDB.sizeDisconnected();
            int potCount = yacyCore.seedDB.sizePotential();
            
            boolean complete = ((post == null) ? false : post.get("links", "false").equals("true"));
            long otherppm = yacyCore.seedDB.countActivePPM();
            long myppm = 0;
            
            // create own peer info
            yacySeed seed = yacyCore.seedDB.mySeed;
            if (yacyCore.seedDB.mySeed != null){ //our Peer
                long links, words;
                try {
                    links = Long.parseLong(seed.get("LCount", "0"));
                    words = Long.parseLong(seed.get("ICount", "0"));
                } catch (Exception e) {links = 0; words = 0;}
                
                prop.put("table_my-name", seed.get("Name", "-") );
                if (yacyCore.seedDB.mySeed.isVirgin()) {
                    prop.put("table_my-type", 0);
                } else if(yacyCore.seedDB.mySeed.isJunior()) {
                    prop.put("table_my-type", 1);
                    accPotLinks += links;
                    accPotWords += words;
                } else if(yacyCore.seedDB.mySeed.isSenior()) {
                    prop.put("table_my-type", 2);
                    accActLinks += links;
                    accActWords += words;
                } else if(yacyCore.seedDB.mySeed.isPrincipal()) {
                    prop.put("table_my-type", 3);
                    accActLinks += links;
                    accActWords += words;
                }
                myppm = seed.getPPM();
                prop.put("table_my-version", seed.get("Version", "-"));
                prop.put("table_my-uptime", serverDate.intervalToString(60000 * Long.parseLong(seed.get("Uptime", ""))));
                prop.put("table_my-links", groupDigits(links));
                prop.put("table_my-words", groupDigits(words));
                prop.put("table_my-acceptcrawl", "" + (seed.getFlagAcceptRemoteCrawl() ? 1 : 0) );
                prop.put("table_my-acceptindex", "" + (seed.getFlagAcceptRemoteIndex() ? 1 : 0) );
                prop.put("table_my-sI", seed.get("sI", "-"));
                prop.put("table_my-sU", seed.get("sU", "-"));
                prop.put("table_my-rI", seed.get("rI", "-"));
                prop.put("table_my-rU", seed.get("rU", "-"));
                prop.put("table_my-ppm", myppm);
                prop.put("table_my-seeds", seed.get("SCount", "-"));
                prop.put("table_my-connects", seed.get("CCount", "-"));
            }
            
            // overall results: Network statistics
            if (iAmActive) conCount++; else if (mySeedType.equals("junior")) potCount++;
            prop.put("table_active-count", conCount);
            prop.put("table_active-links", groupDigits(accActLinks));
            prop.put("table_active-words", groupDigits(accActWords));
            prop.put("table_passive-count", disconCount);
            prop.put("table_passive-links", groupDigits(accPassLinks));
            prop.put("table_passive-words", groupDigits(accPassWords));
            prop.put("table_potential-count", potCount);
            prop.put("table_potential-links", groupDigits(accPotLinks));
            prop.put("table_potential-words", groupDigits(accPotWords));
            prop.put("table_all-count", (conCount + disconCount + potCount));
            prop.put("table_all-links", groupDigits(accActLinks + accPassLinks + accPotLinks));
            prop.put("table_all-words", groupDigits(accActWords + accPassWords + accPotWords));
            
            prop.put("table_gppm", otherppm + ((iAmActive) ? myppm : 0));
            
            String comment = "";
            prop.put("table_comment", 0);
            if (conCount == 0) {
                if (Integer.parseInt(sb.getConfig("onlineMode", "1")) == 2) {
                    prop.put("table_comment", 1);//in onlinemode, but not online
                } else {
                    prop.put("table_comment", 2);//not in online mode, and not online
                }
            }
            prop.put("table", 2); // triggers overview
            prop.put("page", 0);
        } else if (Integer.parseInt(post.get("page", "1")) == 4) {
            prop.put("table", 4); // triggers overview
            prop.put("page", 4);          
            
            if (post.containsKey("addPeer")) {
                
                // AUTHENTICATE
                if (!header.containsKey(httpHeader.AUTHORIZATION)) {
                    prop.put("AUTHENTICATE","log-in");
                    return prop;
                }
                
                
                HashMap map = new HashMap();
                map.put("IP",(String) post.get("peerIP"));
                map.put("Port",(String) post.get("peerPort"));
                yacySeed peer = new yacySeed((String) post.get("peerHash"),map);
                
                int added = yacyClient.publishMySeed(peer.getAddress(), peer.hash);
                
                if (added < 0) {
                    prop.put("table_comment",1);
                    prop.put("table_comment_status","publish: disconnected peer '" + peer.getName() + "/" + post.get("peerHash") + "' from " + peer.getAddress());
                } else {
                    peer = yacyCore.seedDB.getConnected(peer.hash);
                    prop.put("table_comment",2);
                    prop.put("table_comment_status","publish: handshaked " + peer.get("PeerType", "senior") + " peer '" + peer.getName() + "' at " + peer.getAddress());
                    prop.put("table_comment_details",peer.toString());
                }
                
                prop.put("table_peerHash",(String) post.get("peerHash"));
                prop.put("table_peerIP",(String)post.get("peerIP"));
                prop.put("table_peerPort",(String) post.get("peerPort"));                
            } else {
                prop.put("table_peerHash","");
                prop.put("table_peerIP","");
                prop.put("table_peerPort","");                

                prop.put("table_comment",0);
            }
        }else {
            // generate table
            int page = Integer.parseInt(post.get("page", "1"));
            int conCount = 0;
            int maxCount = 500;
            if (yacyCore.seedDB == null) {
                prop.put("table", 0);//no remote senior/principal proxies known"
            } else {
                int size = 0;
                switch (page) {
                    case 1 : size = yacyCore.seedDB.sizeConnected(); break;
                    case 2 : size = yacyCore.seedDB.sizeDisconnected(); break;
                    case 3 : size = yacyCore.seedDB.sizePotential(); break;
                }
                if (size == 0) {
                    prop.put("table", 0);//no remote senior/principal proxies known"
                } else {
                    // add temporary the own seed to the database
                    if (iAmActive) {
                        yacyCore.peerActions.updateMySeed();
                        yacyCore.seedDB.addConnected(yacyCore.seedDB.mySeed);
                    }
                    
                    // find updated Information using YaCyNews
                    HashSet updatedProfile = new HashSet();
                    HashSet updatedWiki = new HashSet();
                    HashMap isCrawling = new HashMap();
                    int availableNews = yacyCore.newsPool.size(yacyNewsPool.INCOMING_DB);
                    if (availableNews > 500) availableNews = 500;
                    yacyNewsRecord record;
                    try {
                        for (int c = availableNews - 1; c >= 0; c--) {
                            record = yacyCore.newsPool.get(yacyNewsPool.INCOMING_DB, c);
                            if (record.category().equals("prfleupd")) {
                                updatedProfile.add(record.originator());
                            } else if (record.category().equals("wiki_upd")) {
                                updatedWiki.add(record.originator());
                            } else if (record.category().equals("crwlstrt")) {
                                isCrawling.put(record.originator(), record.attributes().get("startURL"));
                            }
                        }
                    } catch (IOException e) {}
                    
                    boolean dark = true;
                    yacySeed seed;
                    boolean complete = post.containsKey("ip");
                    Enumeration e = null;
                    switch (page) {
                        case 1 : e = yacyCore.seedDB.seedsSortedConnected(post.get("order", "down").equals("up"), post.get("sort", "LCount")); break;
                        case 2 : e = yacyCore.seedDB.seedsSortedDisconnected(post.get("order", "up").equals("up"), post.get("sort", "LastSeen")); break;
                        case 3 : e = yacyCore.seedDB.seedsSortedPotential(post.get("order", "up").equals("up"), post.get("sort", "LastSeen")); break;
                    }
                    String startURL;
                    int PPM;
                    while ((e.hasMoreElements()) && (conCount < maxCount)) {
                        seed = (yacySeed) e.nextElement();
                        if (seed != null) {
                            if (conCount >= maxCount) break;
                            if (seed.hash.equals(yacyCore.seedDB.mySeed.hash)) {
                                prop.put("table_list_"+conCount+"_dark", 2);
                            } else {
                                prop.put("table_list_"+conCount+"_dark", ((dark) ? 1 : 0) ); dark=!dark;
                            }
                            prop.put("table_list_"+conCount+"_updatedProfile", (((updatedProfile.contains(seed.hash))) ? 1 : 0) );
                            prop.put("table_list_"+conCount+"_updatedWiki", (((updatedWiki.contains(seed.hash))) ? 1 : 0) );
                            try {
                                PPM = Integer.parseInt(seed.get("ISpeed", "-"));
                            } catch (NumberFormatException ee) {
                                PPM = 0;
                            }
                            if (((startURL = (String) isCrawling.get(seed.hash)) == null) || (PPM < 10)) {
                                prop.put("table_list_"+conCount+"_isCrawling", 0);
                                prop.put("table_list_"+conCount+"_isCrawling_startURL", "");
                            } else {
                                prop.put("table_list_"+conCount+"_isCrawling", 1);
                                prop.put("table_list_"+conCount+"_isCrawling_startURL", startURL);
                            }
                            
                            long links, words;
                            try {
                                links = Long.parseLong(seed.get("LCount", "0"));
                                words = Long.parseLong(seed.get("ICount", "0"));
                            } catch (Exception exc) {links = 0; words = 0;}
                            prop.put("table_list_"+conCount+"_complete", ((complete)? 1 : 0) );
                            prop.put("table_list_"+conCount+"_hash", seed.hash);
                            String shortname = seed.get("Name", "deadlink");
                            if (shortname.length() > 20) shortname = shortname.substring(0, 20) + "...";
                            prop.put("table_list_"+conCount+"_shortname", shortname);
                            prop.put("table_list_"+conCount+"_fullname", seed.get("Name", "deadlink"));
                            if (complete) {
                                prop.put("table_list_"+conCount+"_complete", 1);
                                prop.put("table_list_"+conCount+"_complete_ip", seed.get("IP", "-") );
                                prop.put("table_list_"+conCount+"_complete_port", seed.get("Port", "-") );
                                prop.put("table_list_"+conCount+"_complete_hash", seed.hash);
                            }else{
                                prop.put("table_list_"+conCount+"_complete", 0);
                            }
                            if (seed.isJunior()) {
                                prop.put("table_list_"+conCount+"_type", 0);
                            } else if(seed.isSenior()){
                                prop.put("table_list_"+conCount+"_type", 1);
                            } else if(seed.isPrincipal()) {
                                prop.put("table_list_"+conCount+"_type", 2);
                                prop.put("table_list_"+conCount+"_type_url", seed.get("seedURL", "http://nowhere/") );
                            }
                            prop.put("table_list_"+conCount+"_version", yacy.combinedVersionString2PrettyString(seed.get("Version", "0.1")));
                            prop.put("table_list_"+conCount+"_contact", (seed.getFlagDirectConnect() ? 1 : 0) );
                            prop.put("table_list_"+conCount+"_lastSeen", lastSeen(seed.get("LastSeen", "-")) );
                            prop.put("table_list_"+conCount+"_uptime", serverDate.intervalToString(60000 * Long.parseLong(seed.get("Uptime", "0"))));
                            prop.put("table_list_"+conCount+"_links", groupDigits(links));
                            prop.put("table_list_"+conCount+"_words", groupDigits(words));
                            prop.put("table_list_"+conCount+"_acceptcrawl", (seed.getFlagAcceptRemoteCrawl() ? 1 : 0) );
                            prop.put("table_list_"+conCount+"_acceptindex", (seed.getFlagAcceptRemoteIndex() ? 1 : 0) );
                            prop.put("table_list_"+conCount+"_sI", seed.get("sI", "-"));
                            prop.put("table_list_"+conCount+"_sU", seed.get("sU", "-"));
                            prop.put("table_list_"+conCount+"_rI", seed.get("rI", "-"));
                            prop.put("table_list_"+conCount+"_rU", seed.get("rU", "-"));
                            prop.put("table_list_"+conCount+"_ppm", PPM);
                            prop.put("table_list_"+conCount+"_seeds", seed.get("SCount", "-"));
                            prop.put("table_list_"+conCount+"_connects", seed.get("CCount", "-"));
                            conCount++;
                        }//seed != null
                    }//while
                    if (iAmActive) yacyCore.seedDB.removeMySeed();
                    prop.put("table_list", conCount);
                    prop.put("table", 1);
                    prop.put("table_num", conCount);
                    prop.put("table_total", (maxCount > conCount) ? conCount : maxCount);
                    prop.put("table_complete", ((complete)? 1 : 0) );
                }
            }
            prop.put("page", page);
            prop.put("table_page", page);
            switch (page) {
                case 1 : prop.put("table_peertype", "senior/principal"); break;
                case 2 : prop.put("table_peertype", "senior/principal"); break;
                case 3 : prop.put("table_peertype", "junior"); break;
            }
        }
        // return rewrite properties
        return prop;
    }
    
    private static String lastSeen(String date) {
        long l = 0;
        if (date.length() == 0)
            l = 999;
        else
            try {
                l = (yacyCore.universalTime() - yacyCore.shortFormatter.parse(date).getTime()) / 1000 / 60;
            } catch (java.text.ParseException e) {
                l = 999;
            }
        if (l == 999) return "-"; else return "" + l;
    }
    
    private static String groupDigits(long Number) {
        String s = "" + Number;
        String t = "";
        for (int i = 0; i < s.length(); i++)  t = s.charAt(s.length() - i - 1) + (((i % 3) == 0) ? "," : "") + t;
        return t.substring(0, t.length() - 1);
    }
}
