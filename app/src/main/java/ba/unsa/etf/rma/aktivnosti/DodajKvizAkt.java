package ba.unsa.etf.rma.aktivnosti;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
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

import java.util.ArrayList;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.klase.AdapterZaListuMogucihPitanja;
import ba.unsa.etf.rma.klase.AdapterZaListuTrenutnihPitanja;
import ba.unsa.etf.rma.klase.Kategorija;
import ba.unsa.etf.rma.klase.Kviz;
import ba.unsa.etf.rma.klase.Pitanje;

public class DodajKvizAkt extends AppCompatActivity {

    private Button btnDodajKviz;
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
        Pitanje dodajPitanje = new Pitanje();
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

        //Listener na etNaziv vraca pozadinsku boju na defaultnu boju ukoliko korisnik unese bilo sto (potrebno je samo da etNaziv ne bude prazan)
        etNaziv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if( !s.equals("") )
                    etNaziv.setBackgroundColor(Color.parseColor("#fafafa"));
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
    }

}
