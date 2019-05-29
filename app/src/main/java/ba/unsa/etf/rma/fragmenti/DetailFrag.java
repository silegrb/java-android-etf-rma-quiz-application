package ba.unsa.etf.rma.fragmenti;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.aktivnosti.DodajKvizAkt;
import ba.unsa.etf.rma.aktivnosti.IgrajKvizAkt;
import ba.unsa.etf.rma.aktivnosti.KvizoviAkt;
import ba.unsa.etf.rma.klase.AdapterZaListuKvizovaW550;
import ba.unsa.etf.rma.klase.FirebaseKategorije;
import ba.unsa.etf.rma.klase.FirebaseKvizovi;
import ba.unsa.etf.rma.klase.FirebasePitanja;
import ba.unsa.etf.rma.klase.Kviz;
import ba.unsa.etf.rma.klase.Pitanje;

import static android.app.Activity.RESULT_OK;
import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.POSTOJI_LI_KATEGORIJA;
import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.firebasePitanja;
import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.kvizovi;

public class DetailFrag extends Fragment {

    private GridView gridKvizovi;
    private AdapterZaListuKvizovaW550 adapterZaListuKvizovaW550;
    private OnDetailFragmentListener callback;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        gridKvizovi = (GridView) rootView.findViewById( R.id.gridKvizovi );
        adapterZaListuKvizovaW550 = new AdapterZaListuKvizovaW550( getContext(), KvizoviAkt.prikazaniKvizovi);
        gridKvizovi.setAdapter( adapterZaListuKvizovaW550 );
        adapterZaListuKvizovaW550.notifyDataSetChanged();

        gridKvizovi.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Kviz k = (Kviz)parent.getItemAtPosition(position);
                if( !k.getNaziv().equals("Dodaj kviz") ){
                    Intent intent = new Intent(getContext(), IgrajKvizAkt.class);
                    intent.putExtra("odabraniKviz", k );
                    startActivity(intent);
                }
            }
        });

        gridKvizovi.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Intent dodajKvizAkt = new Intent( getContext(), DodajKvizAkt.class );
                dodajKvizAkt.putExtra( "sviKvizovi", kvizovi );
                dodajKvizAkt.putExtra("trenutniKviz", (Kviz)parent.getItemAtPosition(position) );
                dodajKvizAkt.putExtra( "sveKategorije", KvizoviAkt.kategorije );
                for( int i = 0; i < kvizovi.size(); i++ )
                    if( kvizovi.get(i).getNaziv().equals( ((Kviz) parent.getItemAtPosition(position)).getNaziv() ) )
                        KvizoviAkt.pozicijaKviza = i;
                startActivityForResult( dodajKvizAkt, KvizoviAkt.pozicijaKviza );
                return true;
            }
        });

        return rootView;
    }

    public void primiNotifikaciju(String odabir) {
        if( odabir.equals("Svi") ){
            KvizoviAkt.prikazaniKvizovi.clear();
            for( int i = 0; i < kvizovi.size(); i++ )
                KvizoviAkt.prikazaniKvizovi.add( kvizovi.get(i) );
        }
        //Odaberemo li bilo koji drugi element, prikazat ce se svi kvizovi koji pripadaju kategoriji
        //odabranoj u spinneru kategorija.
        else{
            KvizoviAkt.prikazaniKvizovi.clear();
            for( int i = 0; i < kvizovi.size(); i++ )
                if( !kvizovi.get(i).getNaziv().equals("Dodaj kviz") && kvizovi.get(i).getKategorija().getNaziv().equals(odabir) )
                    KvizoviAkt.prikazaniKvizovi.add( kvizovi.get(i) );

            //Filtrirali smo sve potrebne kvizove, potrebno je i dodati element "Dodaj kviz"
            //pomocu kojeg dodajemo novi kviz.

        }
        Kviz k = new Kviz();
        k.setNaziv("Dodaj kviz");
        KvizoviAkt.prikazaniKvizovi.add( k );
        adapterZaListuKvizovaW550.notifyDataSetChanged();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if( requestCode == KvizoviAkt.pozicijaKviza ){
            if( resultCode == RESULT_OK ){
                Kviz kvizZaDodati = (Kviz)data.getExtras().get("noviKviz");
                boolean dodajNovi = (boolean)data.getExtras().get("dodajNoviKviz");
                if( dodajNovi ) {
                    try {
                        NOVI_KVIZ_REGISTRUJ_BAZA_I_APLIKACIJA(kvizZaDodati);
                        adapterZaListuKvizovaW550.notifyDataSetChanged();
                        callback.msg1();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else {
                    ArrayList<Kviz> tempKvizovi = new ArrayList<>();
                    tempKvizovi.addAll( kvizovi );
                    kvizovi.clear();
                    for( int i = 0; i < tempKvizovi.size(); i++ ){
                        if( KvizoviAkt.pozicijaKviza == i ) {
                            kvizovi.add(kvizZaDodati);
                        }
                        else
                            kvizovi.add( tempKvizovi.get(i) );
                    }
                    tempKvizovi.clear();
                }
                KvizoviAkt.prikazaniKvizovi.clear();
                KvizoviAkt.prikazaniKvizovi.addAll( kvizovi );

                Kviz k = new Kviz();
                k.setNaziv("Dodaj kviz");
                KvizoviAkt.prikazaniKvizovi.add( k );
                adapterZaListuKvizovaW550.notifyDataSetChanged();
                callback.msg1();
            }
            else{
                KvizoviAkt.prikazaniKvizovi.clear();
                KvizoviAkt.prikazaniKvizovi.addAll( kvizovi );
                Kviz k = new Kviz();
                k.setNaziv("Dodaj kviz");
                KvizoviAkt.prikazaniKvizovi.add( k );
                adapterZaListuKvizovaW550.notifyDataSetChanged();
                callback.msg1();
            }
        }
    }

    public interface OnDetailFragmentListener {
        void msg1();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof DetailFrag.OnDetailFragmentListener) {
            callback = (DetailFrag.OnDetailFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnGreenFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback = null;
    }

    public void NOVI_KVIZ_REGISTRUJ_BAZA_I_APLIKACIJA(Kviz kvizZaDodati) throws ExecutionException, InterruptedException {
        kvizovi.add( kvizovi.size(), kvizZaDodati );
        FirebaseKvizovi.dodajKviz(kvizZaDodati,getContext());
        ArrayList<Pitanje> pitanjaZaDodati = new ArrayList<>();
        for( int i = 0; i < kvizZaDodati.getPitanja().size(); i++ ) {
            boolean pitanjeVecPostoji = false;
            for (int j = 0; j < firebasePitanja.size(); j++) {
                if (kvizZaDodati.getPitanja().get(i).getNaziv().equals( firebasePitanja.get(j).getNaziv() )) {
                    pitanjeVecPostoji = true;
                }
            }
            if( !pitanjeVecPostoji ) pitanjaZaDodati.add( kvizZaDodati.getPitanja().get(i) );
        }
        FirebasePitanja.dodajPitanja( pitanjaZaDodati, getContext() );
        FirebaseKategorije.dajKategoriju( kvizZaDodati.getKategorija(), getContext() );
        if( !kvizZaDodati.getKategorija().getNaziv().equals("Svi") && !POSTOJI_LI_KATEGORIJA ) {
            FirebaseKategorije.dodajKategoriju(kvizZaDodati.getKategorija(), getContext());
            POSTOJI_LI_KATEGORIJA = true;
        }
    }
}
