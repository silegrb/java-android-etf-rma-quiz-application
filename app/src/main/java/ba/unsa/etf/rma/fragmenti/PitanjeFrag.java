package ba.unsa.etf.rma.fragmenti;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.klase.Kviz;
import ba.unsa.etf.rma.klase.Pitanje;


public class PitanjeFrag extends Fragment {

    private TextView tekstPitanja;
    private ListView odgovoriPitanja;
    private Kviz trenutniKviz;
    private ArrayList<Pitanje> alPitanja = new ArrayList<>();
    private ArrayList<String> alOdgovori = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private Pitanje trenutnoPitanje = null;
    private OnPitanjeFragmentListener callback;
    private int brTacnihOdgovora = 0;
    private int brPreostalihPitanja;
    private boolean pitanjeOdgovoreno = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_pitanje, container, false);
        tekstPitanja = (TextView)rootView.findViewById(R.id.tekstPitanja);
        odgovoriPitanja = (ListView) rootView.findViewById(R.id.odgovoriPitanja);
        trenutniKviz = (Kviz)getArguments().getSerializable("trenutniKviz");
        alPitanja.addAll( trenutniKviz.getPitanja() );
        brPreostalihPitanja = alPitanja.size();
        Collections.shuffle( alPitanja );
        adapter = new ArrayAdapter<String>( getActivity(), android.R.layout.simple_list_item_1, alOdgovori);
        odgovoriPitanja.setAdapter( adapter );
        adapter.notifyDataSetChanged();
        if( alPitanja.size() == 0 )
            tekstPitanja.setText("Kviz je završen!");
        else{
            trenutnoPitanje = alPitanja.get(0);
            tekstPitanja.setText( trenutnoPitanje.getNaziv() );
            alOdgovori.addAll( trenutnoPitanje.dajRandomOdgovore());
            adapter.notifyDataSetChanged();
        }

        odgovoriPitanja.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    if( !pitanjeOdgovoreno ) {

                        pitanjeOdgovoreno = true;

                        if (alPitanja.size() == 0) {
                            tekstPitanja.setText("Kviz je završen!");
                            alOdgovori.clear();
                            adapter.notifyDataSetChanged();
                            callback.messageFromGreenFragment(alPitanja.size(), alPitanja.size() - brPreostalihPitanja, brTacnihOdgovora);
                        } else {

                            if (!parent.getItemAtPosition(position).toString().equals(trenutnoPitanje.getTacan()))
                                view.setBackgroundResource( R.color.crvena );
                                int prvaVidljiva = odgovoriPitanja.getFirstVisiblePosition();
                                int zadnjaVidljiva = odgovoriPitanja.getChildCount();
                                System.out.println(  prvaVidljiva + " " + zadnjaVidljiva );
                            for (int i = 0; i < parent.getChildCount(); i++) {
                                if (trenutnoPitanje.getTacan().equals(parent.getItemAtPosition(i).toString())) {
                                    parent.getChildAt(i).setBackgroundResource( R.color.zelena );
                                    if (i == position) brTacnihOdgovora++;
                                }
                            }
                            alPitanja.remove(trenutnoPitanje);
                            brPreostalihPitanja--;
                            (new Handler()).postDelayed(() -> {
                                for (int i = 0; i < parent.getChildCount(); i++)
                                    parent.getChildAt(i).setBackgroundColor(Color.WHITE);

                                if (alPitanja.size() == 0) {
                                    tekstPitanja.setText("Kviz je završen!");
                                    alOdgovori.clear();
                                    adapter.notifyDataSetChanged();
                                } else {
                                    trenutnoPitanje = alPitanja.get(0);
                                    tekstPitanja.setText(trenutnoPitanje.getNaziv());
                                    alOdgovori.clear();
                                    alOdgovori.addAll(trenutnoPitanje.dajRandomOdgovore());
                                    adapter.notifyDataSetChanged();
                                }
                                pitanjeOdgovoreno = false;
                                callback.messageFromGreenFragment(alPitanja.size() - 1, trenutniKviz.getPitanja().size() - brPreostalihPitanja, brTacnihOdgovora);
                            }, 2000);

                        }
                    }
            }
        });

        return rootView;
    }

    public interface OnPitanjeFragmentListener {
        void messageFromGreenFragment(int brojPreostalih, int brojOdgovorenih, int brojTacnih);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnPitanjeFragmentListener) {
            callback = (OnPitanjeFragmentListener) context;
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
}
