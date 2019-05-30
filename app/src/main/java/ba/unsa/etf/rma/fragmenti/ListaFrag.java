package ba.unsa.etf.rma.fragmenti;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.concurrent.ExecutionException;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.aktivnosti.KvizoviAkt;
import ba.unsa.etf.rma.klase.AdapterZaListuKategorija;

public class ListaFrag extends Fragment {

    private ListView listaKategorija;
    private AdapterZaListuKategorija adapterZaListuKategorija;
    private OnListaFragmentListener callback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_lista, container, false);

        listaKategorija = (ListView) rootView.findViewById(R.id.listaKategorija);
        adapterZaListuKategorija = new AdapterZaListuKategorija(getContext(), KvizoviAkt.kategorije);
        listaKategorija.setAdapter(adapterZaListuKategorija);
        adapterZaListuKategorija.notifyDataSetChanged();
        listaKategorija.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String text = parent.getItemAtPosition( position ).toString();
                try {
                    callback.msg( text );
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Toast.makeText( parent.getContext(), "Odabrano: " + text, Toast.LENGTH_SHORT ).show();
            }
        });
        return rootView;
    }

    public void primiNotifikaciju() {
        adapterZaListuKategorija.notifyDataSetChanged();
    }

    public interface OnListaFragmentListener {
        void msg(String odabir) throws ExecutionException, InterruptedException;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ListaFrag.OnListaFragmentListener) {
            callback = (ListaFrag.OnListaFragmentListener) context;
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
