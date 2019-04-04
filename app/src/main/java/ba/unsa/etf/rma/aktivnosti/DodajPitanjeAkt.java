package ba.unsa.etf.rma.aktivnosti;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.klase.AdapterZaListuOdgovora;
import ba.unsa.etf.rma.klase.Pitanje;

public class DodajPitanjeAkt extends AppCompatActivity {

    Button dodajTacan;
    Button dodajOdgovor;
    Button dodajPitanje;
    ListView lvOdgovori;
    EditText etNaziv;
    EditText etOdgovor;
    ArrayList<String> alOdgovori = new ArrayList<>();
    AdapterZaListuOdgovora adapterZaListuOdgovora;
    Pitanje trenutnoPitanje =  new Pitanje();
    public static boolean tacanDodan = false;
    public static String tacanOdgovor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dodaj_pitanje_akt);

        dodajTacan = (Button) findViewById( R.id.btnDodajTacan );
        dodajOdgovor = (Button) findViewById( R.id.btnDodajOdgovor );
        dodajPitanje = (Button) findViewById( R.id.btnDodajPitanje );
        lvOdgovori = (ListView) findViewById( R.id.lvOdgovori );
        etNaziv = (EditText) findViewById( R.id.etNaziv );
        etOdgovor = (EditText) findViewById( R.id.etOdgovor );
        adapterZaListuOdgovora = new AdapterZaListuOdgovora(this, alOdgovori);
        lvOdgovori.setAdapter( adapterZaListuOdgovora );

        dodajTacan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( !tacanDodan && !etOdgovor.getText().toString().equals("") ){
                    trenutnoPitanje.setTacan( etOdgovor.getText().toString() );
                    alOdgovori.add( etOdgovor.getText().toString() );
                    tacanOdgovor = etOdgovor.getText().toString();
                    trenutnoPitanje.setOdgovori( alOdgovori );
                    etOdgovor.setText("");
                    adapterZaListuOdgovora.notifyDataSetChanged();
                    tacanDodan = true;

                }
            }
        });

        dodajOdgovor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( !etOdgovor.getText().toString().equals("") ){
                    alOdgovori.add( etOdgovor.getText().toString() );
                    trenutnoPitanje.setOdgovori( alOdgovori );
                    etOdgovor.setText("");
                    adapterZaListuOdgovora.notifyDataSetChanged();
                }
            }
        });
    }
}
