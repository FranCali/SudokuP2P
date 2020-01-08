package it.unisa.studenti.dao;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.FuturePut;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.Number640;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.storage.Data;
import net.tomp2p.utils.Pair;
import net.tomp2p.dht.PeerDHT;

public class PlayerDAO {
    private static Random random = new Random();

    @SuppressWarnings("unchecked")
    public static HashMap<String, Object[]> getGamePlayers(PeerDHT peer, String gameName){
        String key = gameName + "_players";
        try{
            FutureGet futureGet = peer.get(Number160.createHash(key)).getLatest().start();
            futureGet.awaitUninterruptibly();
            
            if(futureGet.isSuccess() && !futureGet.isEmpty()){
                return (HashMap<String, Object[]>) futureGet.dataMap().values().iterator().next().object();
            }
            else{
                return null;
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static boolean initPlayers(PeerDHT peer, String gameName, String nickname, Integer score){
        HashMap<String, Object[]> players = new HashMap<>();
        String key = gameName + "_players";
        Object[] playerInfo = new Object[2];
        playerInfo[0] = peer.peer().peerAddress();
        playerInfo[1] = score;
        players.put(nickname, playerInfo);
        try{
            FutureGet futureGet = peer.get(Number160.createHash(gameName)).getLatest().start();
            futureGet.awaitUninterruptibly();
            
    
            if(futureGet.isSuccess() && !futureGet.isEmpty()){
                peer.put(Number160.createHash(key)).data(new Data(players)).start().awaitUninterruptibly(); 
                
                return true;
            }
            else if(futureGet.isSuccess()) {
                return false;
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }

    public static boolean storePlayer(PeerDHT peerDHT, String gameName, String nickname, Integer score, Boolean isDeletion) throws ClassNotFoundException, InterruptedException, IOException{
        Pair<Number640, Byte> pair2 = null;
		for (int i = 0; i < 5; i++) {
            Pair<Number160, Data> pair = getAndUpdatePlayer(peerDHT, gameName, nickname, score, isDeletion);
			if (pair == null) {
				return false;
			}
			FuturePut fp = peerDHT
					.put(Number160.createHash(gameName + "_players"))
                    .data(pair.element1().prepareFlag(), pair.element0())
                    .start().awaitUninterruptibly();
            
            pair2 = checkVersions(fp.rawResult());
			// 1 is PutStatus.OK_PREPARED
			if (pair2 != null && pair2.element1() == 1) {
				break;
			}
			System.out.println("get delay or fork - put");
			// if not removed, a low ttl will eventually get rid of it
			peerDHT.remove(Number160.createHash(gameName + "_players")).versionKey(pair.element0()).start()
                    .awaitUninterruptibly();
            
			Thread.sleep(random.nextInt(500));
		}
		if (pair2 != null && pair2.element1() == 1) {
			peerDHT.put(Number160.createHash(gameName + "_players"))
					.versionKey(pair2.element0().versionKey()).putConfirm()
                    .data(new Data()).start().awaitUninterruptibly();
            
            return true;
		} else {
			System.out
                    .println("we cannot handle this kind of inconsistency automatically, handing over the the API dev");
            return false;
		}
    }

    @SuppressWarnings("unchecked")
    private static Pair<Number160, Data> getAndUpdatePlayer(PeerDHT peerDHT, String gameName, String nickname, Integer score, Boolean isDeletion) throws InterruptedException, ClassNotFoundException, IOException {

        Pair<Number640, Data> pair = null;
		for (int i = 0; i < 5; i++) {
            FutureGet fg = peerDHT.get(Number160.createHash(gameName + "_players")).getLatest().start().awaitUninterruptibly();
            
            if(fg.isSuccess() && fg.isEmpty())
                return null;
            // check if all the peers agree on the same latest version, if not
			// wait a little and try again
            pair = checkVersions(fg.rawData());
			if (pair != null) {
				break;
			}
			System.out.println("get delay or fork - get");
			Thread.sleep(random.nextInt(500));
		}
		// we got the latest data
		if (pair != null) {
            
            HashMap<String, Object[]> players = (HashMap<String, Object[]>) pair.element1().object();
            
            if(isDeletion == false){
                Object[] playerInfo = new Object[2]; //array containing player address and player score
                playerInfo[0] = peerDHT.peer().peerAddress();
                playerInfo[1] = score;
                players.put(nickname, playerInfo);
            }
            else{
                if(players.containsKey(nickname)) 
                    players.remove(nickname); 
                else
                    return null;
            }
            
			Data newData = new Data(players);
			Number160 v = pair.element0().versionKey();
            long version = v.timestamp() + 1;
			newData.addBasedOn(v);
            //since we create a new version, we can access old versions as well
			return new Pair<Number160, Data>(new Number160(version, newData.hash()), newData);
		}
		return null;
    }

    private static <K> Pair<Number640, K> checkVersions(Map<PeerAddress, Map<Number640, K>> rawData) {
		Number640 latestKey = null;
		K latestData = null;
		for (Map.Entry<PeerAddress, Map<Number640, K>> entry : rawData.entrySet()) {
			if (latestData == null && latestKey == null) {
				latestData = entry.getValue().values().iterator().next();
                latestKey = entry.getValue().keySet().iterator().next();
			} else {
				if (!latestKey.equals(entry.getValue().keySet().iterator()
						.next())
						|| !latestData.equals(entry.getValue().values()
								.iterator().next())) {
					return null;
				}
			}
		}
		return new Pair<Number640, K>(latestKey, latestData);
	}

}