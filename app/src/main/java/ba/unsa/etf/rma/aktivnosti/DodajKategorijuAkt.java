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
import android.widget.Toast;

import com.maltaisn.icondialog.Icon;
import com.maltaisn.icondialog.IconDialog;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.klase.FirebaseKategorije;
import ba.unsa.etf.rma.klase.Kategorija;

import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.POSTOJI_LI_KATEGORIJA;

public class DodajKategorijuAkt extends AppCompatActivity implements IconDialog.Callback {

    private EditText etNaziv;
    private EditText etIkona;
    private Button dodajIkonu;
    private Button dodajKategoriju;
    private ArrayList<Kategorija> kategorije;
    private Icon[] selectedIcons;
    public static boolean dodajKat = false;
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
                    etNaziv.setBackgroundResource( R.drawable.button_border );
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
                dodajKat = true;
                String s1 = "", s2 = "";
                if( etNaziv.getText().toString().equals("") ) {

                    dodajKat = false;
                    s1 += "Unesite kategoriju!";
                }


                    if( true  ) {
                        dodajKat = false;
                        Kategorija k = new Kategorija();
                        k.setNaziv( etNaziv.getText().toString() );
                        etIkona.setEnabled(true);
                        String ikona = etIkona.getText().toString();
                        etIkona.setEnabled(false);
                        if( ikona.equals("") ) ikona = "958";
                        k.setId( ikona );
                        try {
                            if( !etNaziv.getText().toString().equals("Svi") && !etNaziv.getText().toString().equals("Dodaj kategoriju") )
                            FirebaseKategorije.dajKategoriju(k,getApplicationContext());
                            POSTOJI_LI_KATEGORIJA = true;
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if(!dodajKat )
                            s2 += "Kategorija vec postoji!";
                    }
                if( !dodajKat )   {
                    String konacni = "";
                    if( !s1.equals("") ) konacni += s1;
                    int trenutnaDuzina = konacni.length();
                    if( !konacni.equals("") ) konacni += "\n";
                    if( !s2.equals("") ) konacni += s2;
                    int novaDuzina = konacni.length();
                    if( trenutnaDuzina + 1 == novaDuzina ) konacni = s1;
                    etNaziv.setBackgroundColor( Color.parseColor("#ff0006") );
                    Toast.makeText( v.getContext(), konacni, Toast.LENGTH_SHORT).show();

                }
                else{
                    Kategorija k = new Kategorija();
                    k.setNaziv( etNaziv.getText().toString() );
                    etIkona.setEnabled(true);
                    String ikona = etIkona.getText().toString();
                    etIkona.setEnabled(false);
                    if( ikona.equals("") ) ikona = "958";
                    k.setId( ikona );

                    try {
                        FirebaseKategorije.dodajKategoriju(k,getApplicationContext());
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
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
