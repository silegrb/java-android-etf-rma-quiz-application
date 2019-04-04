package ba.unsa.etf.rma.klase;

public class Kategorija {

    private String naziv;
    private String id;

    public String getNaziv() {
        return naziv;
    }

    public void setNaziv(String naziv) {
        this.naziv = naziv;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return naziv;
    }
}
