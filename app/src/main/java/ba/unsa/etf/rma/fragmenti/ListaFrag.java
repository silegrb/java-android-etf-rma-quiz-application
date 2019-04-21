package ba.unsa.etf.rma.fragmenti;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.aktivnosti.KvizoviAkt;
import ba.unsa.etf.rma.klase.AdapterZaListuKategorija;

public class ListaFrag extends Fragment {

    private ListView listaKategorija;
    private AdapterZaListuKategorija adapterZaListuKategorija;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_lista, container, false);

        listaKategorija = (ListView) rootView.findViewById( R.id.listaKategorija );
        adapterZaListuKategorija = new AdapterZaListuKategorija( getContext(), KvizoviAkt.kategorije );
        listaKategorija.setAdapter( adapterZaListuKategorija );
        adapterZaListuKategorija.notifyDataSetChanged();

        return rootView;
    }
}
