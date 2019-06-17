package ba.unsa.etf.rma.klase;

import android.content.Context;
import android.database.Cursor;
import android.provider.CalendarContract;
import android.util.Pair;

public class KalendarEventi {

    private Context context;
    private long primljeniPocetakMillis;
    private long primljeniKrajMillis;

    public KalendarEventi( Context context, long primljeniPocetakMillis, long primljeniKrajMillis ){
        this.context = context;
        this.primljeniPocetakMillis = primljeniPocetakMillis;
        this.primljeniKrajMillis = primljeniKrajMillis;
    }

    public Pair<Pair<Boolean,String>,Pair<Long,String>> provjeriEvente(){

        String[] uzmi = new String[]{
                CalendarContract.Events.DTSTART, CalendarContract.Events.DTEND,CalendarContract.Events.TITLE};
        Cursor kursorZaEvente;
        try{
            kursorZaEvente = context.getContentResolver().query(CalendarContract.Events.CONTENT_URI, uzmi, null, null, null);
            while( kursorZaEvente.moveToNext() ){
                long pocetakEventa = kursorZaEvente.getLong(0);
                long krajEventa = kursorZaEvente.getLong(1);
                String imeEventa = kursorZaEvente.getString(2);
                //Tri slucaja postoje!
                if( (primljeniPocetakMillis < krajEventa && primljeniPocetakMillis > pocetakEventa) || ( pocetakEventa < primljeniPocetakMillis && primljeniKrajMillis < krajEventa )) return new Pair<>(new Pair<>(true,"In progress"),new Pair<>(new Long(0),imeEventa));
                if( pocetakEventa < primljeniKrajMillis && krajEventa > primljeniKrajMillis ) return new Pair<>(new Pair<>(true,"Will start"),new Pair<>(pocetakEventa-primljeniPocetakMillis,imeEventa));
            }

            return new Pair<>(new Pair<>(false,""),new Pair<>(new Long(0),""));
        }
        catch(SecurityException e){
            e.printStackTrace();
        }

        return null;
    }

}
