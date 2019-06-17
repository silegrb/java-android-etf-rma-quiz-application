package ba.unsa.etf.rma.klase;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import ba.unsa.etf.rma.aktivnosti.KvizoviAkt;

import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.RANG_LISTE;
import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.firebasePitanja;
import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.kategorije;
import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.kvizovi;

public class SQLiteBaza extends SQLiteOpenHelper {

    private String query = "";
    public SQLiteBaza(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }
    public SQLiteBaza(Context context)
    {
        super(context, "lokalnaBaza.sqLiteDatabase", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        kreirajTabele(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        pobrisiTabele(db);
        kreirajTabele(db);
    }

    public void kreirajTabele(SQLiteDatabase db){

        query = "CREATE TABLE Kategorije ( _id TEXT PRIMARY KEY, idIkonice TEXT NOT NULL );";
        db.execSQL(query);
        query = "CREATE TABLE Kvizovi ( _id TEXT PRIMARY KEY,nazivKviza TEXT NOT NULL, idKategorije TEXT NOT NULL, CONSTRAINT constr_idKatKviza FOREIGN KEY ( idKategorije ) REFERENCES Kategorije ( _id ) );";
        db.execSQL(query);
        query = "CREATE TABLE Pitanja ( _id TEXT PRIMARY KEY, indexTacnogOdgovora INTEGER NOT NULL, odgovori TEXT NOT NULL);";
        db.execSQL(query);
        query = "CREATE TABLE RangListe ( _id TEXT PRIMARY KEY, igraci TEXT NOT NULL, rezultati TEXT NOT NULL, idKviza TEXT NOT NULL, CONSTRAINT constr_idKvizaRL FOREIGN KEY ( idKviza ) REFERENCES Kvizovi ( _id ) );";
        db.execSQL(query);
        query = "CREATE TABLE PitanjeUKvizu ( idKviza TEXT NOT NULL, idPitanja TEXT NOT NULL, CONSTRAINT constr_idKviza FOREIGN KEY ( idKviza ) REFERENCES Kvizovi ( _id ), CONSTRAINT constr_idPitanja FOREIGN KEY ( idPitanja ) REFERENCES Pitanje ( _id ) );";
        db.execSQL(query);
    }

    public void pobrisiTabele( SQLiteDatabase db ){
        query = "DROP TABLE IF EXISTS PitanjeUKvizu";
        db.execSQL(query);
        query = "DROP TABLE IF EXISTS RangListe";
        db.execSQL(query);
        query = "DROP TABLE IF EXISTS Kvizovi";
        db.execSQL(query);
        query = "DROP TABLE IF EXISTS Pitanja";
        db.execSQL(query);
        query = "DROP TABLE IF EXISTS Kategorije";
        db.execSQL(query);
    }

    public void dodajKategoriju( Kategorija k ){

            String nazivKategorije = k.getNaziv();
            String nazivKategorijeNemaKoseRazmake = nazivKategorije.replaceAll( " ", "_RAZMAK_" );
            String nazivKategorijeNemaKoseRazmakeNemaKoseCrte = nazivKategorijeNemaKoseRazmake.replaceAll( "/", "_KOSA_CRTA_" );
            ContentValues contentValues = new ContentValues();
            contentValues.put("_id", nazivKategorijeNemaKoseRazmakeNemaKoseCrte );
            contentValues.put("idIkonice", k.getId() );
            SQLiteDatabase db = this.getWritableDatabase();
            db.insert("Kategorije", null, contentValues);
    }

    public void dodajPitanje( Pitanje p ){

                String nazivPitanja = p.getNaziv();
                String nazivPitanjaNemaKoseRazmake = nazivPitanja.replaceAll( " ", "_RAZMAK_" );
                String nazivPitanjaNemaKoseRazmakeNemaKoseCrte = nazivPitanjaNemaKoseRazmake.replaceAll( "/", "_KOSA_CRTA_" );
                int indexTacnogOdgovora = -1;
                String odgovori = "";
                for( int i = 0; i < p.getOdgovori().size(); i++ ) {
                    odgovori += p.getOdgovori().get(i);
                    if( i < p.getOdgovori().size() - 1 ) odgovori += ";";
                    if (p.getOdgovori().get(i).equals(p.getTacan()))
                        indexTacnogOdgovora = i;
                }
                ContentValues contentValues = new ContentValues();
                contentValues.put("_id", nazivPitanjaNemaKoseRazmakeNemaKoseCrte);
                contentValues.put("indexTacnogOdgovora", indexTacnogOdgovora);
                contentValues.put("odgovori", odgovori);
                SQLiteDatabase db = this.getWritableDatabase();
                db.insert("Pitanja", null, contentValues);
    }

    public void dodajKviz( Kviz k, RangListaKlasa rangListaKlasa ){
        String nazivKategorije = k.getKategorija().getNaziv();
        String nazivKategorijeNemaRazmake = nazivKategorije.replaceAll( " ", "_RAZMAK_" );
        String nazivKategorijeNemaRazmakeNemaKoseCrte = nazivKategorijeNemaRazmake.replaceAll( "/", "_KOSA_CRTA_" );
        ContentValues contentValues = new ContentValues();
        contentValues.put("_id",k.getNEPROMJENJIVI_ID());
        contentValues.put("nazivKviza",k.getNaziv());
        contentValues.put("idKategorije", nazivKategorijeNemaRazmakeNemaKoseCrte);
        SQLiteDatabase db = this.getWritableDatabase();
        db.insert("Kvizovi",null,contentValues);
        contentValues = new ContentValues();
        contentValues.put("_id",rangListaKlasa.getNEPROMJENJIVI_ID());
        contentValues.put("igraci","");
        contentValues.put("rezultati","");
        contentValues.put("idKviza",k.getNEPROMJENJIVI_ID());
        db.insert("RangListe",null,contentValues);
        for( int i = 0; i < k.getPitanja().size(); i++ ) {
            String nazivPitanja = k.getPitanja().get(i).getNaziv();
            String nazivPitanjaNemaRazmake = nazivPitanja.replaceAll( " ", "_RAZMAK_" );
            String nazivPitanjaNemaRazmakeNemaKoseCrte = nazivPitanjaNemaRazmake.replaceAll( "/", "_KOSA_CRTA_" );
            contentValues = new ContentValues();
            contentValues.put("idKviza",k.getNEPROMJENJIVI_ID());
            contentValues.put("idPitanja",nazivPitanjaNemaRazmakeNemaKoseCrte);
            db.insert("PitanjeUKvizu",null,contentValues);
        }

    }

    public void dodajPitanja(ArrayList<Pitanje> pitanja){
        for( int i = 0; i < pitanja.size(); i++ )
            dodajPitanje( pitanja.get(i) );
    }

    public void registrujRezultatIgranjaKviza(Kviz k, RangListaKlasa rangListaKlasa){
        ContentValues staEditujem = new ContentValues();
        String rezultati = "";
        String igraci = "";
        for(Map.Entry<Integer,Pair<String,Double>> entry : rangListaKlasa.getMapa().entrySet()) {

            Pair<String, Double> podaciOPokusaju = entry.getValue();
            String igrac = podaciOPokusaju.first;
            String rezultat = String.valueOf( podaciOPokusaju.second );
            igraci += igrac;
            igraci += ";";
            rezultati += rezultat;
            rezultati += ";";
        }
        if( !igraci.equals("") ) igraci = igraci.substring(0, igraci.length() - 1);
        if( !rezultati.equals("") ) rezultati = rezultati.substring(0, rezultati.length() - 1);
        staEditujem.put("igraci",igraci);
        staEditujem.put("rezultati",rezultati);
        staEditujem.put("idKviza",k.getNEPROMJENJIVI_ID());
        String uslov = "idKviza = '" + k.getNEPROMJENJIVI_ID() + "';";
        SQLiteDatabase db = this.getWritableDatabase();
        db.update("RangListe", staEditujem, uslov, null);
    }

    public void editujKviz( Kviz k ){
        //Treba editovat kviz u bazi
        //Treba obrisat iz tabele N:N kvizovi-pitanja i dodat ih opet
        SQLiteDatabase db = this.getWritableDatabase();
        String uslov = "idKviza = '" + k.getNEPROMJENJIVI_ID() + "';";
        String whereArgs[] = null;
        db.delete("PitanjeUKvizu", uslov, whereArgs);
        for( int i = 0; i < k.getPitanja().size(); i++ ) {
            String nazivPitanja = k.getPitanja().get(i).getNaziv();
            String nazivPitanjaNemaRazmake = nazivPitanja.replaceAll( " ", "_RAZMAK_" );
            String nazivPitanjaNemaRazmakeNemaKoseCrte = nazivPitanjaNemaRazmake.replaceAll( "/", "_KOSA_CRTA_" );
            ContentValues contentValues = new ContentValues();
            contentValues.put("idKviza",k.getNEPROMJENJIVI_ID());
            contentValues.put("idPitanja",nazivPitanjaNemaRazmakeNemaKoseCrte);
            db.insert("PitanjeUKvizu",null,contentValues);
        }
        String nazivKategorije = k.getKategorija().getNaziv();
        String nazivKategorijeNemaKoseRazmake = nazivKategorije.replaceAll( " ", "_RAZMAK_" );
        String nazivKategorijeNemaKoseRazmakeNemaKoseCrte = nazivKategorijeNemaKoseRazmake.replaceAll( "/", "_KOSA_CRTA_" );
        ContentValues staEditujem = new ContentValues();
        staEditujem.put("nazivKviza", k.getNaziv());
        staEditujem.put("idKategorije",nazivKategorijeNemaKoseRazmakeNemaKoseCrte);
        uslov = "_id = " + k.getNEPROMJENJIVI_ID() + ";";
        db.update("Kvizovi",staEditujem,uslov,null);
    }

    public void pokupiKategorije() {

            String[] kategorijeKolone = new String[]{"_id", "idIkonice"};
            SQLiteDatabase db = this.getReadableDatabase();
            Kategorija kategorijaSvi = new Kategorija();
            kategorijaSvi.setNaziv("Svi");
            kategorijaSvi.setId("5");
            kategorije.add(kategorijaSvi);

            Cursor cursor = null;
            try {
                cursor = db.query("Kategorije", kategorijeKolone, null, null, null, null, null);
                while (cursor.moveToNext()) {
                    Kategorija pokupljenaKategorija = new Kategorija();
                    String nazivPokupljeneKategorije = cursor.getString( cursor.getColumnIndexOrThrow("_id")  );
                    String nazivKategorijeSaKosimCrtama = nazivPokupljeneKategorije.replaceAll( "_KOSA_CRTA_", "/" );
                    String nazivKategorijeSaKosimCrtamaSaRazmacima = nazivKategorijeSaKosimCrtama.replaceAll( "_RAZMAK_", " " );
                    String idIkonicePokupljeneKategorije = cursor.getString( cursor.getColumnIndexOrThrow("idIkonice") );
                    pokupljenaKategorija.setNaziv( nazivKategorijeSaKosimCrtamaSaRazmacima );
                    pokupljenaKategorija.setId( idIkonicePokupljeneKategorije );
                    KvizoviAkt.kategorije.add( pokupljenaKategorija );
                }
                cursor.close();
            }
            catch (Exception e){
                e.printStackTrace();
            }
    }

    public void pokupiPitanja() {

        String[] pitanjaKolone = new String[]{"_id", "indexTacnogOdgovora","odgovori"};
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = null;
        try {
            cursor = db.query("Pitanja", pitanjaKolone, null, null, null, null, null);
            while (cursor.moveToNext()) {
                Pitanje pokupljenoPitanje = new Pitanje();
                String nazivPokupljenogPitanja = cursor.getString( cursor.getColumnIndexOrThrow("_id")  );
                String nazivPitanjaSaKosimCrtama = nazivPokupljenogPitanja.replaceAll( "_KOSA_CRTA_", "/" );
                String nazivPitanjaSaKosimCrtamaSaRazmacima = nazivPitanjaSaKosimCrtama.replaceAll( "_RAZMAK_", " " );
                int indexTacnogOdgovora = cursor.getInt( cursor.getColumnIndexOrThrow("indexTacnogOdgovora") );
                String odgovori_STRING = cursor.getString( cursor.getColumnIndexOrThrow("odgovori") );
                String[] odgovori_SPLIT = odgovori_STRING.split(";");
                ArrayList<String> odgovori = new ArrayList<>();
                for( int i = 0; i < odgovori_SPLIT.length; i++ )
                    odgovori.add( odgovori_SPLIT[i] );
                pokupljenoPitanje.setNaziv(nazivPitanjaSaKosimCrtamaSaRazmacima);
                pokupljenoPitanje.setTacan( odgovori.get( indexTacnogOdgovora ) );
                pokupljenoPitanje.setOdgovori( odgovori );
                pokupljenoPitanje.setTekstPitanja( nazivPitanjaSaKosimCrtamaSaRazmacima );
                firebasePitanja.add( pokupljenoPitanje );
            }
            cursor.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void pokupiKvizove() {
        String[] kvizoviKolone = new String[]{"_id", "nazivKviza","idKategorije"};
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = null;
        try {
            cursor = db.query("Kvizovi", kvizoviKolone, null, null, null, null, null);
            while (cursor.moveToNext()) {
                Kviz pokupljeniKviz = new Kviz();
                String id = cursor.getString( cursor.getColumnIndexOrThrow("_id") );
                String nazivKviza = cursor.getString( cursor.getColumnIndexOrThrow("nazivKviza") );
                String idKategorije = cursor.getString( cursor.getColumnIndexOrThrow("idKategorije") );
                String idKategorijeSaKosimCrtama = idKategorije.replaceAll( "_KOSA_CRTA_", "/" );
                String idKategorijeSaKosimCrtamaSaRazmacima = idKategorijeSaKosimCrtama.replaceAll( "_RAZMAK_", " " );
                ArrayList<String> naziviPitanjaUKvizu = dajPripadajucaPitanja(id);
                ArrayList<Pitanje> pitanjaUKvizu = new ArrayList<>();
                for( int i = 0; i < firebasePitanja.size(); i++ )
                    for( int j = 0; j < naziviPitanjaUKvizu.size(); j++ )
                        if( naziviPitanjaUKvizu.get(j).equals( firebasePitanja.get(i).getNaziv() ) )
                            pitanjaUKvizu.add( firebasePitanja.get(i) );

                for( int i = 0; i < kategorije.size(); i++ )
                    if( kategorije.get(i).getNaziv().equals( idKategorijeSaKosimCrtamaSaRazmacima ) )
                        pokupljeniKviz.setKategorija( kategorije.get(i) );
                pokupljeniKviz.setNEPROMJENJIVI_ID( id );
                pokupljeniKviz.setNaziv( nazivKviza );
                pokupljeniKviz.setPitanja( pitanjaUKvizu );
                kvizovi.add( pokupljeniKviz );
            }
            cursor.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public ArrayList<String> dajPripadajucaPitanja(String idKviza){
        ArrayList<String> pripadajucaPitanja = new ArrayList<>();
        query = "SELECT idPitanja FROM PitanjeUKvizu WHERE idKviza = '" + idKviza + "';";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try{
            cursor = db.rawQuery(query, null);
            while( cursor.moveToNext() ){
                String idPitanja = cursor.getString( cursor.getColumnIndexOrThrow("_id")  );
                String idPitanjaSaKosimCrtama = idPitanja.replaceAll( "_KOSA_CRTA_", "/" );
                String idPitanjaSaKosimCrtamaSaRazmacima = idPitanjaSaKosimCrtama.replaceAll( "_RAZMAK_", " " );
                pripadajucaPitanja.add( idPitanjaSaKosimCrtamaSaRazmacima );
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return pripadajucaPitanja;
    }

    public void pokupiRangliste() {
        String[] ranglisteKolone = new String[]{"_id","igraci", "rezultati","idKviza"};
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = null;
        try {
            cursor = db.query("RangListe", ranglisteKolone, null, null, null, null, null);
            while (cursor.moveToNext()) {
                RangListaKlasa rangListaKlasa = new RangListaKlasa();

                String idRangListe = cursor.getString( cursor.getColumnIndexOrThrow("_id")  );
                String igraci_STRING = cursor.getString( cursor.getColumnIndexOrThrow("igraci")  );
                String rezultati_STRING = cursor.getString( cursor.getColumnIndexOrThrow("rezultati")  );
                String idKviza = cursor.getString( cursor.getColumnIndexOrThrow("idKviza")  );
                for( int i = 0; i < kvizovi.size(); i++ )
                    if( kvizovi.get(i).getNEPROMJENJIVI_ID().equals(idKviza) )
                        rangListaKlasa.setNazivKviza( kvizovi.get(i).getNaziv() );
                String[] igraci_SPLIT = igraci_STRING.split(";");
                String[] rezultati_SPLIT = rezultati_STRING.split(";");
                ArrayList<Pair<String,Double>> igracRezultat = new ArrayList<>();

                for( int i = 0; i < igraci_SPLIT.length; i++ ){
                    Pair<String,Double> par = new Pair<>( igraci_SPLIT[i], Double.valueOf( rezultati_SPLIT[i] ) );
                    igracRezultat.add( par );
                }

                Map<Integer,Pair<String,Double>> mojaMapa = new TreeMap<>();
                for( int i = 0; i < igracRezultat.size(); i++ )
                    mojaMapa.put(i+1, igracRezultat.get(i));
                rangListaKlasa.setMapa(mojaMapa);
                rangListaKlasa.setNEPROMJENJIVI_ID(idRangListe);
                RANG_LISTE.add( rangListaKlasa );
            }
            cursor.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
