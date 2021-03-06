package ba.unsa.etf.rma.klase;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.maltaisn.icondialog.IconHelper;

import java.util.ArrayList;

import ba.unsa.etf.rma.R;

public class AdapterZaListuKvizova extends BaseAdapter implements View.OnClickListener, Parcelable {

    private Context context;
    private ArrayList<Kviz> data;
    private static LayoutInflater inflater = null;
    private ImageView ikonaClanaListe;

    public AdapterZaListuKvizova(Context context, ArrayList<Kviz> data) {
        // TODO Auto-generated constructor stub
        this.context = context;
        this.data = data;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    protected AdapterZaListuKvizova(Parcel in) {
    }

    public static final Creator<AdapterZaListuKvizova> CREATOR = new Creator<AdapterZaListuKvizova>() {
        @Override
        public AdapterZaListuKvizova createFromParcel(Parcel in) {
            return new AdapterZaListuKvizova(in);
        }

        @Override
        public AdapterZaListuKvizova[] newArray(int size) {
            return new AdapterZaListuKvizova[size];
        }
    };

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        View vi = convertView;
        if (vi == null)
            vi = inflater.inflate(R.layout.element_liste_kvizova, null);
        TextView text = vi.findViewById(R.id.Itemname);
        ikonaClanaListe = vi.findViewById( R.id.icon );
        text.setText( data.get(position).getNaziv() );
        final IconHelper iconHelper = IconHelper.getInstance(vi.getContext());
        final View finalVi = vi;
        iconHelper.addLoadCallback(new IconHelper.LoadCallback() {
            @Override
            public void onDataLoaded() {
                // This happens on UI thread, and is guaranteed to be called.
                    if( !data.get(position).getNaziv().equals("Dodaj kviz") )
                        ikonaClanaListe.setImageDrawable(iconHelper.getIcon(Integer.parseInt(data.get(position).getKategorija().getId())).getDrawable(context));
                    else
                        ikonaClanaListe.setImageDrawable( finalVi.getResources().getDrawable( R.drawable.addicon ) );
                    notifyDataSetChanged();
            }
        });
        return vi;
    }

    @Override
    public void onClick(View v) {
        //Do nothing
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }
}
