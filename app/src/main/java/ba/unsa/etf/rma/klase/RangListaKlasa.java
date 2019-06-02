package ba.unsa.etf.rma.klase;

import android.util.Pair;

import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class RangListaKlasa {
    private String NEPROMJENJIVI_ID;
    private String nazivKviza;
    private Map<Integer,Pair<String,Double>> mapa = new TreeMap<>();
    public RangListaKlasa(){
        Date datum = new Date();
        NEPROMJENJIVI_ID = String.valueOf( datum.getDay() ) + String.valueOf( datum.getMonth() )
                + String.valueOf( datum.getYear() ) + String.valueOf( datum.getHours() ) +
                String.valueOf( datum.getMinutes() ) + String.valueOf( datum.getSeconds() );
    }
    public void registrujKorisnika(Pair<String,Double> podaciOdigraniKviz){
        Integer odredjivanjePozicije = mapa.size() + 1;
        Map<Integer,Pair<String,Double>> novaMapa = new TreeMap<Integer, Pair<String,Double>>();
        for(Map.Entry<Integer,Pair<String,Double>> entry : mapa.entrySet()) {
            Integer pozicijaPokusaja = entry.getKey();
            Pair<String,Double> podaciOPokusaju = entry.getValue();
            Double procenatTacnih = podaciOdigraniKviz.second;
            if( podaciOPokusaju.second < procenatTacnih ){
                odredjivanjePozicije--;
                novaMapa.put( ++pozicijaPokusaja,podaciOPokusaju );
            }
            else
                novaMapa.put( pozicijaPokusaja,podaciOPokusaju );
        }
        novaMapa.put( odredjivanjePozicije, podaciOdigraniKviz );
        mapa.clear();

        mapa.putAll( novaMapa );
    }

    public Map<Integer, Pair<String, Double>> getMapa() {
        return mapa;
    }

    public void setMapa(Map<Integer, Pair<String, Double>> mapa) {
        this.mapa = mapa;
    }

    public String getNazivKviza() {
        return nazivKviza;
    }

    public void setNazivKviza(String nazivKviza) {
        this.nazivKviza = nazivKviza;
    }

    public String getNEPROMJENJIVI_ID() {
        return NEPROMJENJIVI_ID;
    }

    public void setNEPROMJENJIVI_ID(String NEPROMJENJIVI_ID) {
        this.NEPROMJENJIVI_ID = NEPROMJENJIVI_ID;
    }
}