package ba.unsa.etf.rma.aktivnosti;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.maltaisn.icondialog.Icon;
import com.maltaisn.icondialog.IconDialog;

import java.util.ArrayList;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.klase.Kategorija;

public class DodajKategorijuAkt extends AppCompatActivity implements IconDialog.Callback {

    private EditText etNaziv;
    private EditText etIkona;
    private Button dodajIkonu;
    private Button dodajKategoriju;
    private ArrayList<Kategorija> kategorije;
    private Icon[] selectedIcons;
    //public boolean pritisnutnoSpasiKategoriju = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dodaj_kategoriju_akt);
        etNaziv = (EditText) findViewById( R.id.etNaziv );
        etIkona = (EditText) findViewById( R.id.etIkona );
        dodajIkonu = (Button) findViewById( R.id.btnDodajIkonu );
        dodajKategoriju = (Button) findViewById( R.id.btnDodajKategoriju );
        kategorije = (ArrayList<Kategorija>)getIntent().getSerializableExtra("sveKategorije");
        etIkona.setEnabled(false);

        final IconDialog iconDialog = new IconDialog();
        etNaziv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean postoji = false;
                for ( Kategorija k : kategorije)
                    if( k.getNaziv().equals( etNaziv.getText().toString() ) )
                        postoji = true;

                if( !postoji && !etNaziv.getText().toString().equals("") )
                    etNaziv.setBackgroundColor(Color.parseColor("#fafafa") );
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        dodajIkonu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iconDialog.setSelectedIcons(selectedIcons);
                iconDialog.show(getSupportFragmentManager(), "icon_dialog");
            }
        });

        dodajKategoriju.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean dodajKat = true;

                if( etNaziv.getText().toString().equals("") )
                    dodajKat = false;

                for( Kategorija k: kategorije )
                    if( k.getNaziv().equals( etNaziv.getText().toString() ) )
                        dodajKat = false;
                if( !dodajKat )   {
                    etNaziv.setBackgroundColor( Color.parseColor("#ff0006") );
                }
                else{
                    //pritisnutnoSpasiKategoriju = true;
                    Kategorija k = new Kategorija();
                    k.setNaziv( etNaziv.getText().toString() );
                    etIkona.setEnabled(true);
                    k.setId( etIkona.getText().toString() );
                    etIkona.setEnabled(false);
                    KvizoviAkt.kategorije.add( kategorije.size() - 1, k );
                    Intent resIntent = new Intent();
                    resIntent.putExtra("novaKategorija", k );
                    setResult(RESULT_OK, resIntent);
                    finish();
                }
            }
        });
    }

    @Override
    public void onIconDialogIconsSelected(Icon[] icons) {
        selectedIcons = icons;
        etIkona.setEnabled(true);
        if( selectedIcons != null ) {
            int id = selectedIcons[0].getId();
            etIkona.setText(Integer.toString(id));
        }
        etIkona.setEnabled(false);
    }
}
