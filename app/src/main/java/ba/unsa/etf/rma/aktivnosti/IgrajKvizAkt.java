package ba.unsa.etf.rma.aktivnosti;

import android.content.Intent;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import java.util.Date;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.fragmenti.InformacijeFrag;
import ba.unsa.etf.rma.fragmenti.PitanjeFrag;
import ba.unsa.etf.rma.klase.Kviz;

public class IgrajKvizAkt extends AppCompatActivity implements PitanjeFrag.OnPitanjeFragmentListener {

    InformacijeFrag informacijeFrag;
    PitanjeFrag pitanjeFrag;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_igraj_kviz_akt);
        Bundle bundle = new Bundle();
        Kviz k = (Kviz)getIntent().getSerializableExtra("odabraniKviz");
        Date vrijemeAlarma = (Date)getIntent().getSerializableExtra("vrijemeAlarma");
        Intent alarm = new Intent(AlarmClock.ACTION_SET_ALARM);
        alarm.putExtra(AlarmClock.EXTRA_MESSAGE, "Alarm za kraj igranja kviza \"" + k.getNaziv() + "\"");
        alarm.putExtra(AlarmClock.EXTRA_HOUR, vrijemeAlarma.getHours());
        alarm.putExtra(AlarmClock.EXTRA_MINUTES, vrijemeAlarma.getMinutes());
        alarm.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        IgrajKvizAkt.this.startActivity(alarm);
        bundle.putSerializable( "trenutniKviz", k  );
        informacijeFrag = new InformacijeFrag();
        pitanjeFrag = new PitanjeFrag();
        informacijeFrag.setArguments( bundle );
        pitanjeFrag.setArguments( bundle );

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add( R.id.informacijePlace, informacijeFrag );
        fragmentTransaction.add( R.id.pitanjePlace, pitanjeFrag );
        fragmentTransaction.commit();
    }

    @Override
    public void messageFromGreenFragment(int brojPreostalih, int brojOdgovorenih, int brojTacnih) {
        informacijeFrag.primiNotifikaciju( brojPreostalih, brojOdgovorenih, brojTacnih );
    }

}

