package ba.unsa.etf.rma.klase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class Kviz implements Serializable {

    private String naziv;
    private ArrayList<Pitanje> pitanja;
    private Kategorija kategorija;
    private String NEPROMJENJIVI_ID;

    public Kviz(){
        Date datum = new Date();
        NEPROMJENJIVI_ID = String.valueOf( datum.getDay() ) + String.valueOf( datum.getMonth() )
                + String.valueOf( datum.getYear() ) + String.valueOf( datum.getHours() ) +
                String.valueOf( datum.getMinutes() ) + String.valueOf( datum.getSeconds() );
    }

    public String getNEPROMJENJIVI_ID() {
        return NEPROMJENJIVI_ID;
    }

    public void setNEPROMJENJIVI_ID(String NEPROMJENJIVI_ID) {
        this.NEPROMJENJIVI_ID = NEPROMJENJIVI_ID;
    }

    public String getNaziv() {
        return naziv;
    }

    public void setNaziv(String naziv) {
        this.naziv = naziv;
    }

    public ArrayList<Pitanje> getPitanja() {
        return pitanja;
    }

    public void setPitanja(ArrayList<Pitanje> pitanja) {
        this.pitanja = pitanja;
    }

    public Kategorija getKategorija() {
        return kategorija;
    }

    public void setKategorija(Kategorija kategorija) {
        this.kategorija = kategorija;
    }

    public void dodajPitanje( Pitanje pitanje ){
        pitanja.add( pitanje );
    }
}
