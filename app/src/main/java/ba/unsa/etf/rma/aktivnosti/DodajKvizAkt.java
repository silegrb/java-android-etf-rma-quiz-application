package ba.unsa.etf.rma.aktivnosti;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.klase.AdapterZaListuMogucihPitanja;
import ba.unsa.etf.rma.klase.AdapterZaListuTrenutnihPitanja;
import ba.unsa.etf.rma.klase.Kategorija;
import ba.unsa.etf.rma.klase.Kviz;
import ba.unsa.etf.rma.klase.Pitanje;

public class DodajKvizAkt extends AppCompatActivity {

    private static final int READ_REQUEST_CODE = 42;
    private Button btnDodajKviz;
    private Button btnImportKviz;
    private EditText etNaziv;
    private ArrayList<Kategorija> kategorije;
    private ArrayList<Kviz> kvizovi;
    private Kviz trenutniKviz;
    private ArrayAdapter<Kategorija> adapterZaSpinner;
    private Spinner kategorijeSpinner;
    private ListView lvTrenutnihPitanja;
    private ListView lvMogucihPitanja;
    private ArrayList<Pitanje>  alTrenutnaPitanja = new ArrayList<>();
    private ArrayList<Pitanje>  alMogucaPitanja = new ArrayList<>();
    private AdapterZaListuTrenutnihPitanja adapterZaListuTrenutnihPitanja;
    private AdapterZaListuMogucihPitanja adapterZaListuMogucihPitanja;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dodaj_kviz_akt);

        //Dodajmo sve vrijednosti koristeci id.
        etNaziv = ( EditText ) findViewById( R.id.etNaziv );
        btnDodajKviz = (Button) findViewById( R.id.btnDodajKviz );
        btnImportKviz = (Button) findViewById( R.id.btnImportKviz );
        kategorijeSpinner = (Spinner)findViewById(R.id.spKategorije );
        lvMogucihPitanja = (ListView) findViewById( R.id.lvMogucaPitanja );
        lvTrenutnihPitanja = (ListView) findViewById( R.id.lvDodanaPitanja );

        //Kupljenje podataka iz intenta.
        trenutniKviz = (Kviz)getIntent().getSerializableExtra("trenutniKviz");
        kategorije = (ArrayList<Kategorija>)getIntent().getSerializableExtra("sveKategorije");
        kvizovi = (ArrayList<Kviz>)getIntent().getSerializableExtra("sviKvizovi");

        //Postavljanje adaptera.
        adapterZaSpinner = new ArrayAdapter<Kategorija>(this, android.R.layout.simple_list_item_1, kategorije);
        kategorijeSpinner.setAdapter( adapterZaSpinner );
        adapterZaListuTrenutnihPitanja = new AdapterZaListuTrenutnihPitanja( this, alTrenutnaPitanja );
        lvTrenutnihPitanja.setAdapter( adapterZaListuTrenutnihPitanja );
        adapterZaListuMogucihPitanja = new AdapterZaListuMogucihPitanja( this, alMogucaPitanja );
        lvMogucihPitanja.setAdapter( adapterZaListuMogucihPitanja );

        //Dodavanje imaginarne kategorije, pomocu kojeg mozemo dodati novu kategoriju.
        final Kategorija kategorijaZaDodavanje = new Kategorija();
        kategorijaZaDodavanje.setNaziv("Dodaj kategoriju");
        kategorije.add( kategorijaZaDodavanje );

        //Ukoliko je trenutniKviz apstraktni kviz za dodavanje, samo spinner kategorija postavljamo na svi, a
        //ako nije (else dio) onda cemo sve vrijednosti postaviti na podatke odabranog kviza.
        if( trenutniKviz.getNaziv().equals("Dodaj kviz") )
            kategorijeSpinner.setSelection(0);
        else {
            int position = -1;
            for( int i = 0; i < kategorije.size(); i++ )
                if( kategorije.get(i).getNaziv().equals( trenutniKviz.getKategorija().getNaziv() ) ){
                    position = i;
                    break;
                }
            kategorijeSpinner.setSelection( position );
            alTrenutnaPitanja.addAll(trenutniKviz.getPitanja());
            etNaziv.setText( trenutniKviz.getNaziv() );
        }

        //Dodajmo u listu trenutnih pitanja i apstraktni element pomocu kojeg mozemo dodati i novo pitanje.
        final Pitanje dodajPitanje = new Pitanje();
        dodajPitanje.setNaziv("Dodaj pitanje");
        alTrenutnaPitanja.add( dodajPitanje );
        adapterZaListuTrenutnihPitanja.notifyDataSetChanged();

        //Akcija pritiska elementa lvTrenutnihPitanja prebacuje pritisnuti element u listu mogucih pitanja.
        lvTrenutnihPitanja.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Pitanje odabrano = (Pitanje) parent.getItemAtPosition( position );
                if( !odabrano.getNaziv().equals("Dodaj pitanje") ) {
                    alTrenutnaPitanja.remove(odabrano);
                    alMogucaPitanja.add(odabrano);
                }
                else{
                    Intent dodajPitanjeAkt = new Intent( DodajKvizAkt.this, DodajPitanjeAkt.class );
                    dodajPitanjeAkt.putExtra("trenutniKviz", trenutniKviz);
                    dodajPitanjeAkt.putExtra("trenutnaPitanja", alTrenutnaPitanja);
                    dodajPitanjeAkt.putExtra("mogucaPitanja", alMogucaPitanja);
                    DodajKvizAkt.this.startActivityForResult( dodajPitanjeAkt, 777 );
                }
                adapterZaListuMogucihPitanja.notifyDataSetChanged();
                adapterZaListuTrenutnihPitanja.notifyDataSetChanged();
            }
        });

        //Akcija pritiska elementa lvMogucihPitanja prebacuje pritisnuti element u listu trenutnih pitanja.
        lvMogucihPitanja.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Pitanje odabrano = (Pitanje) parent.getItemAtPosition( position );
                alMogucaPitanja.remove(odabrano);
                    alTrenutnaPitanja.add(alTrenutnaPitanja.size() - 1,odabrano);

                adapterZaListuMogucihPitanja.notifyDataSetChanged();
                adapterZaListuTrenutnihPitanja.notifyDataSetChanged();
            }
        });

        //Dugme ispod vrsi validaciju unesenih podataka za novi kviz, te ukoliko je validacija zadovoljena
        //dodajemo novi kviz u listu svih kvizova.
        btnDodajKviz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String uredjivaniKviz = "";

                if( etNaziv.getText().toString().equals("") ){
                    etNaziv.setBackgroundColor(Color.parseColor("#ff0006"));
                    Toast.makeText(v.getContext(), "Unesi ime kviza!", Toast.LENGTH_SHORT ).show();
                }
                else{
                    boolean dodajNoviKviz = false, vecPostoji = false;
                    if( trenutniKviz.getNaziv().equals("Dodaj kviz") )
                        dodajNoviKviz = true;

                    if( dodajNoviKviz ){
                        for( Kviz k: kvizovi )
                            if( k.getNaziv().equals( etNaziv.getText().toString() ) )
                                vecPostoji = true;
                    }
                    else {
                        uredjivaniKviz = trenutniKviz.getNaziv();
                        for( Kviz k: kvizovi )
                            if( !k.getNaziv().equals( uredjivaniKviz ) && k.getNaziv().equals( etNaziv.getText().toString() ) )
                                vecPostoji = true;
                    }

                    if( etNaziv.getText().toString().equals("Dodaj kviz") )
                        vecPostoji = true;

                    if( vecPostoji ){
                        etNaziv.setBackgroundColor(Color.parseColor("#ff0006"));
                        Toast.makeText(v.getContext(), "Kviz vec postoji!", Toast.LENGTH_SHORT ).show();
                    }
                    else {
                        Kviz povratniKviz = new Kviz();
                        for (int i = 0; i < alTrenutnaPitanja.size(); i++)
                            if (alTrenutnaPitanja.get(i).getNaziv().equals("Dodaj pitanje")) {
                                alTrenutnaPitanja.remove(i);
                                i--;
                            }
                        Kategorija kategorija = (Kategorija) kategorijeSpinner.getSelectedItem();
                        povratniKviz.setKategorija(kategorija);
                        povratniKviz.setNaziv(etNaziv.getText().toString());
                        povratniKviz.setPitanja(alTrenutnaPitanja);
                        Intent resIntent = new Intent();
                        resIntent.putExtra("noviKviz", povratniKviz);
                        resIntent.putExtra("dodajNoviKviz", dodajNoviKviz);
                        setResult(RESULT_OK, resIntent);
                        finish();
                    }
                }

            }
        });

        btnImportKviz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent( Intent.ACTION_OPEN_DOCUMENT );
                intent.addCategory( Intent.CATEGORY_OPENABLE );
                intent.setType( "text/*" );
                startActivityForResult( intent, READ_REQUEST_CODE );
            }
        });

        //Listener na etNaziv vraca pozadinsku boju na defaultnu boju ukoliko korisnik unese bilo sto (potrebno je samo da etNaziv ne bude prazan)
        etNaziv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if( !s.equals("") )
                    etNaziv.setBackgroundResource(R.drawable.button_border);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        //Ukoliko pritisnemo na kategoriju 'Dodaj kategoriju' pokrece se nova aktivnost za dodavanje nove kategorije.
       kategorijeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
           @Override
           public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
               if( parent.getItemAtPosition(position).toString().equals("Dodaj kategoriju") ){
                   Intent dodajKategorijuAkt = new Intent( DodajKvizAkt.this, DodajKategorijuAkt.class );
                   dodajKategorijuAkt.putExtra("sveKategorije", kategorije );
                   DodajKvizAkt.this.startActivityForResult( dodajKategorijuAkt, 69 );
               }
           }

           @Override
           public void onNothingSelected(AdapterView<?> parent) {
                //Do nothing
           }
       });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if( requestCode == 777 ){
            if( resultCode == RESULT_OK ){
                Pitanje pitanje = (Pitanje) data.getExtras().get("novoPitanje");
                alTrenutnaPitanja.add( alTrenutnaPitanja.size() - 1, pitanje );
                adapterZaListuMogucihPitanja.notifyDataSetChanged();
                adapterZaListuTrenutnihPitanja.notifyDataSetChanged();
            }
        }
        if( requestCode == 69 ){
            if( resultCode == RESULT_OK ){
                Kategorija k = (Kategorija) data.getExtras().get("novaKategorija");
                kategorije.add( kategorije.size() - 1, k );
                kategorijeSpinner.setSelection( kategorije.size() - 2 );
                adapterZaSpinner.notifyDataSetChanged();
            }
            else{
                kategorijeSpinner.setSelection( kategorije.size() - 2 );
                adapterZaSpinner.notifyDataSetChanged();
            }
        }
        if( requestCode == READ_REQUEST_CODE ){
            if( resultCode == RESULT_OK ){
                Uri uri = null;
                if (data != null) {
                    uri = data.getData();
                    try {
                        //Ovdje ide provjera validnosti.
                        String textFileString = readTextFromUri(uri);
                        String[] nizStringova = textFileString.split(",");
                        boolean importuj = true;

                        if( textFileString.equals("") ){
                            importuj = false;
                            AlertDialog alertDialog = new AlertDialog.Builder(DodajKvizAkt.this).create();
                            alertDialog.setTitle("Upozorenje");
                            alertDialog.setMessage("Datoteka je prazna");
                            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });

                            alertDialog.show();
                        }
                        else if( nizStringova.length < 5 ){
                            importuj = false;
                            AlertDialog alertDialog = new AlertDialog.Builder(DodajKvizAkt.this).create();
                            alertDialog.setTitle("Upozorenje");
                            alertDialog.setMessage("Datoteka nije ispravnog formata");
                            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });

                            alertDialog.show();
                        }
                        else {
                            boolean kvizVecPostoji = false;
                            if( nizStringova[0].equals("Dodaj kviz") ) kvizVecPostoji = true;
                            for( Kviz k: kvizovi )
                                if( k.getNaziv().equals( nizStringova[0] ) )
                                    kvizVecPostoji = true;

                            if( kvizVecPostoji ) {
                                importuj = false;
                                AlertDialog alertDialog = new AlertDialog.Builder(DodajKvizAkt.this).create();
                                alertDialog.setTitle("Upozorenje");
                                alertDialog.setMessage("Kviz kojeg importujete već postoji!");
                                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                alertDialog.show();
                            }
                            int brojOdgovora = Integer.parseInt( nizStringova[2] );
                            int indeksTacnogOdgovora = Integer.parseInt( nizStringova[3] );
                            if( brojOdgovora + 4 != nizStringova.length ){
                                importuj = false;
                                AlertDialog alertDialog = new AlertDialog.Builder(DodajKvizAkt.this).create();
                                alertDialog.setTitle("Upozorenje");
                                alertDialog.setMessage("Kviz kojeg importujete ima neispravan broj odgovora!");
                                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                alertDialog.show();
                            }
                            if( indeksTacnogOdgovora < 0 || indeksTacnogOdgovora > brojOdgovora - 1 ){
                                importuj = false;
                                AlertDialog alertDialog = new AlertDialog.Builder(DodajKvizAkt.this).create();
                                alertDialog.setTitle("Upozorenje");
                                alertDialog.setMessage("Kviz kojeg importujete ima neispravan index tačnog odgovora!");
                                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                alertDialog.show();
                            }

                            if( importuj ){
                                //Prvo provjeravamo da li kategorija postoji, te ako ne postoji dodajemo je.
                                boolean kategorijaVecPostoji = false;
                                for( Kategorija k: KvizoviAkt.kategorije )
                                    if( k.getNaziv().equals( nizStringova[1] ) )
                                        kategorijaVecPostoji = true;

                                if( !kategorijaVecPostoji ) {
                                    Kategorija kategorija = new Kategorija();
                                    kategorija.setNaziv( nizStringova[1] );
                                    kategorija.setId("958");
                                    KvizoviAkt.kategorije.add( kategorije.size() - 1, kategorija );
                                    kategorije.add( kategorije.size() - 1, kategorija );
                                    kategorijeSpinner.setSelection( kategorije.size() - 2 );
                                }
                                else{
                                    for( int i = 0; i < kategorije.size(); i++ )
                                        if( kategorije.get(i).getNaziv().equals( nizStringova[1] ) ) {
                                            kategorijeSpinner.setSelection(i);
                                            break;
                                        }
                                }
                               // adapterZaSpinner.notifyDataSetChanged();
                                etNaziv.getText().clear();
                                etNaziv.setText( nizStringova[0] );
                                alTrenutnaPitanja.clear();
                                alMogucaPitanja.clear();
                                Pitanje p = new Pitanje();
                                p.setNaziv("TestPitanje");
                                ArrayList<String> odgovori = new ArrayList<>();
                                for( int i = 4; i < nizStringova.length; i++ )
                                    odgovori.add( nizStringova[i] );
                                p.setOdgovori( odgovori );
                                p.setTacan( odgovori.get( indeksTacnogOdgovora ) );
                                alTrenutnaPitanja.add( p );
                                Pitanje pDp = new Pitanje();
                                pDp.setNaziv("Dodaj pitanje");
                                alTrenutnaPitanja.add( pDp );
                                adapterZaSpinner.notifyDataSetChanged();
                                adapterZaListuTrenutnihPitanja.notifyDataSetChanged();
                                adapterZaListuMogucihPitanja.notifyDataSetChanged();
                            }

                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private String readTextFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        inputStream.close();
        reader.close();
        return stringBuilder.toString();
    }

}
