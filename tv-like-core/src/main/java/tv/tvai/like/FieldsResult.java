package tv.tvai.like;

public abstract class FieldsResult {

    private FieldResult text;
    private FieldResult img;
    private FieldResult link;

    public FieldResult getText() {
        return text;
    }

    public void setText(FieldResult text) {
        this.text = text;
    }

    public FieldResult getImg() {
        return img;
    }

    public void setImg(FieldResult img) {
        this.img = img;
    }

    public FieldResult getLink() {
        return link;
    }

    public void setLink(FieldResult link) {
        this.link = link;
    }

    public boolean hasFieldContent() {
        return text != null || img != null || link != null;
    }
}
