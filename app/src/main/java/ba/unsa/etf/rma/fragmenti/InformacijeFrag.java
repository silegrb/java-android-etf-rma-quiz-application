package ba.unsa.etf.rma.fragmenti;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.klase.Kviz;

public class InformacijeFrag extends Fragment {

    private TextView infNazivKviza;
    private TextView infBrojTacnihPitanja;
    private TextView infBrojPreostalihPitanja;
    private TextView infProcenatTacni;
    private Button btnKraj;
    private Kviz trenutniKviz;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_informacije, container, false);
        infNazivKviza = (TextView)rootView.findViewById(R.id.infNazivKviza);
        infBrojTacnihPitanja = (TextView)rootView.findViewById(R.id.infBrojTacnihPitanja);
        infBrojPreostalihPitanja = (TextView)rootView.findViewById(R.id.infBrojPreostalihPitanja);
        infProcenatTacni = (TextView)rootView.findViewById(R.id.infProcenatTacni);
        btnKraj = (Button)rootView.findViewById(R.id.btnKraj);
        trenutniKviz = (Kviz)getArguments().getSerializable("trenutniKviz");
        infNazivKviza.setText( trenutniKviz.getNaziv() );
        if( trenutniKviz.getPitanja().size() == 0 )
            infBrojPreostalihPitanja.setText( "0" );
        else
            infBrojPreostalihPitanja.setText( String.valueOf( trenutniKviz.getPitanja().size() - 1 ) );
        infBrojTacnihPitanja.setText( "0" );
        infProcenatTacni.setText("0.0%");
        btnKraj.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });
        return rootView;
    }

    public void primiNotifikaciju(int brojPreostalih, int brojOdgovorenih, int brojTacnih) {
        if( brojPreostalih == -1 )
            infBrojPreostalihPitanja.setText( "0" );
        else
            infBrojPreostalihPitanja.setText( String.valueOf( brojPreostalih ) );
        infBrojTacnihPitanja.setText( String.valueOf( brojTacnih ) );
        double pomoc = (double)brojTacnih/brojOdgovorenih;
        pomoc *= 100;
        pomoc = Math.round( pomoc*100.0 )/100.0;
        String brojNaDvijeDecimale = String.valueOf(pomoc);
        infProcenatTacni.setText( brojNaDvijeDecimale + "%" );
    }

}
