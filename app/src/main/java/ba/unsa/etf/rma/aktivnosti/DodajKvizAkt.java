package ba.unsa.etf.rma.aktivnosti;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import java.util.ArrayList;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.klase.AdapterZaListuPitanja;
import ba.unsa.etf.rma.klase.Kategorija;
import ba.unsa.etf.rma.klase.Kviz;
import ba.unsa.etf.rma.klase.Pitanje;

public class DodajKvizAkt extends AppCompatActivity {

    ArrayList<Kategorija> kategorije;
    ArrayList<Kviz> kvizovi;
    Kviz trenutniKviz;
    ArrayAdapter<Kategorija> adapterZaSpinner;
    Spinner kategorijeSpinner;
    ListView lvTrenutnihPitanja;
    ListView lvMogucihPitanja;
    ArrayList<Pitanje>  alTrenutnaPitanja = new ArrayList<>();
    ArrayList<Pitanje>  alMogucaPitanja = new ArrayList<>();
    AdapterZaListuPitanja adapterZaListuPitanja;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dodaj_kviz_akt);

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
        }
        if( !trenutniKviz.getNaziv().equals("Dodaj kviz") ) {
            alTrenutnaPitanja.addAll(trenutniKviz.getPitanja());
            Pitanje dodajPitanje = new Pitanje();
            dodajPitanje.setNaziv("Dodaj pitanje");
            alTrenutnaPitanja.add( dodajPitanje );
        }
        adapterZaListuPitanja = new AdapterZaListuPitanja(this, alTrenutnaPitanja);
        lvTrenutnihPitanja.setAdapter( adapterZaListuPitanja );
        adapterZaListuPitanja.notifyDataSetChanged();


    }
}
