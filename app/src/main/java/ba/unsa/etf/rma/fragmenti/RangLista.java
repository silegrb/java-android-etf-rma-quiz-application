package ba.unsa.etf.rma.fragmenti;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.klase.Kviz;

import static ba.unsa.etf.rma.aktivnosti.KvizoviAkt.RANG_LISTE;

public class RangLista extends Fragment {

    private ListView rangListaKviza;
    private ArrayList<String> rezultati = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private Kviz trenutniKviz;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_rang_lista, container, false);
        rangListaKviza = (ListView) rootView.findViewById(R.id.rangListaKviza);
        trenutniKviz = (Kviz)getArguments().getSerializable("trenutniKviz");
      //  boolean
        for( int i = 0; i < RANG_LISTE.size(); i++ )
      //  PopuniListuRezultatima popuniListuRezultatima = new PopuniListuRezultatima(getContext(),trenutniKviz.getNaziv());
        adapter = new ArrayAdapter<String>( getActivity(), android.R.layout.simple_list_item_1, rezultati);
        rangListaKviza.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        return rootView;
    }

    public class PopuniListuRezultatima extends AsyncTask<String,Void,Void>{

        private Context context;
        private String nazivKviza;

        public PopuniListuRezultatima(Context context,String nazivKviza){
            this.context = context;
            this.nazivKviza = nazivKviza;
        }

        @Override
        protected Void doInBackground(String... strings) {

            return null;
        }
    }

}
