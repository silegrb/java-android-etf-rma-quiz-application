package ba.unsa.etf.rma.aktivnosti;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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

    Button btnDodajKviz;
    EditText etNaziv;
    ArrayList<Kategorija> kategorije;
    ArrayList<Kviz> kvizovi;
    Kviz trenutniKviz;
    ArrayAdapter<Kategorija> adapterZaSpinner;
    Spinner kategorijeSpinner;
    ListView lvTrenutnihPitanja;
    ListView lvMogucihPitanja;
    ArrayList<Pitanje>  alTrenutnaPitanja = new ArrayList<>();
    ArrayList<Pitanje>  alMogucaPitanja = new ArrayList<>();
    AdapterZaListuTrenutnihPitanja adapterZaListuTrenutnihPitanja;
    AdapterZaListuMogucihPitanja adapterZaListuMogucihPitanja;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dodaj_kviz_akt);

        etNaziv = ( EditText ) findViewById( R.id.etNaziv );
        btnDodajKviz = (Button) findViewById( R.id.btnDodajKviz );
        kategorijeSpinner = (Spinner)findViewById(R.id.spKategorije );
        lvMogucihPitanja = (ListView) findViewById( R.id.lvMogucaPitanja );
        lvTrenutnihPitanja = (ListView) findViewById( R.id.lvDodanaPitanja );

        kategorije = (ArrayList<Kategorija>)getIntent().getSerializableExtra("sveKategorije");
        kvizovi = (ArrayList<Kviz>)getIntent().getSerializableExtra("sviKvizovi");
        trenutniKviz = (Kviz)getIntent().getSerializableExtra("trenutniKviz");
        Kategorija kategorijaZaDodavanje = new Kategorija();
        kategorijaZaDodavanje.setNaziv("Dodaj kategoriju");
        kategorije.add( kategorijaZaDodavanje );
        adapterZaSpinner = new ArrayAdapter<Kategorija>(this, android.R.layout.simple_list_item_1, kategorije);
        kategorijeSpinner.setAdapter( adapterZaSpinner );
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
        if( !trenutniKviz.getNaziv().equals("Dodaj kviz") ) {
            //Obrisi ovaj if
        }
        Pitanje dodajPitanje = new Pitanje();
        dodajPitanje.setNaziv("Dodaj pitanje");
        alTrenutnaPitanja.add( dodajPitanje );
        adapterZaListuTrenutnihPitanja = new AdapterZaListuTrenutnihPitanja( this, alTrenutnaPitanja );
        adapterZaListuMogucihPitanja = new AdapterZaListuMogucihPitanja( this, alMogucaPitanja );
        lvTrenutnihPitanja.setAdapter( adapterZaListuTrenutnihPitanja );
        lvMogucihPitanja.setAdapter( adapterZaListuMogucihPitanja );
        adapterZaListuTrenutnihPitanja.notifyDataSetChanged();
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
                    DodajKvizAkt.this.startActivity( dodajPitanjeAkt );
                }
                adapterZaListuMogucihPitanja.notifyDataSetChanged();
                adapterZaListuTrenutnihPitanja.notifyDataSetChanged();
            }
        });

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

        btnDodajKviz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( etNaziv.getText().toString().equals("") ){
                    etNaziv.setBackgroundColor(Color.parseColor("#ff0006"));
                    Toast.makeText(v.getContext(), "Unesi ime kviza!", Toast.LENGTH_SHORT ).show();
                }

            }
        });

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

       kategorijeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
           @Override
           public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
               if( parent.getItemAtPosition(position).toString().equals("Dodaj kategoriju") ){
                   Intent dodajKategorijuAkt = new Intent( DodajKvizAkt.this, DodajKategorijuAkt.class );
                   dodajKategorijuAkt.putExtra("sveKategorije", kategorije );
                   DodajKvizAkt.this.startActivity( dodajKategorijuAkt );
               }
           }

           @Override
           public void onNothingSelected(AdapterView<?> parent) {
                //Do nothing
           }
       });
    }
}
