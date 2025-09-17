package com.hexedrealms.visuicomponents;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;

public class SliderActor extends Table {  // Используем Table вместо VerticalGroup для лучшего контроля
    private final VisLabel nameLabel;
    private final VisLabel valueLabel;
    private final Table sliderRow;

    private String [] interpreted;
    public final Slider slider;

    public SliderActor(String name, float minValue, float maxValue, float step, float currentValue) {
        super();
        this.align(Align.left);

        // Создаём элементы
        nameLabel = new VisLabel(name);
        nameLabel.setAlignment(Align.left);

        valueLabel = new VisLabel();
        valueLabel.setStyle(VisUI.getSkin().get("withback", Label.LabelStyle.class));
        valueLabel.setAlignment(Align.center, Align.center);
        valueLabel.setText(String.format("%.0f", currentValue*100)+"%");

        slider = new Slider(minValue, maxValue, step, false, VisUI.getSkin());
        slider.setValue(currentValue);

        // 1-я строка: название параметра
        this.add(nameLabel)
            .left()
            .padBottom(5)
            .row();

        // 2-я строка: слайдер (50%) + значение (оставшееся место)
        sliderRow = new Table();
        sliderRow.add(slider).padRight(10); // Фиксируем ширину слайдера
        sliderRow.add(valueLabel).expandX().right(); // Значение растягивается вправо
        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if(interpreted != null){
                    valueLabel.setText(interpreted[(int) slider.getValue()]);
                    return;
                }
                valueLabel.setText(String.format("%.0f", slider.getValue()*100)+"%");
            }
        });


        this.add(sliderRow)
            .growX()
            .row();
    }

    public void setText(String text){
        valueLabel.setText(text);
    }

    public void putInterpreted(String [] interpreted){
        this.interpreted = interpreted;
        valueLabel.setText(interpreted[(int) slider.getValue()]);
    }

    protected void updateButtonStyle() {
        Slider.SliderStyle sliderStyle = VisUI.getSkin().get("default-horizontal", Slider.SliderStyle.class);

        float width = this.getWidth();
        sliderRow.getCell(slider).width(width * 0.4f);
        sliderStyle.background.setMinWidth(width * 0.4f);
        sliderStyle.background.setMinHeight(width * 0.06f);
        sliderStyle.knob.setMinWidth(width * 0.08f);
        sliderStyle.knob.setMinHeight(width * 0.05f);
        updateTextureFilter(sliderStyle.knob);
        updateTextureFilter(sliderStyle.background);
    }

    protected void updateTextureFilter(Drawable drawable) {
        if (drawable instanceof TextureRegionDrawable) {
            TextureRegionDrawable textureRegionDrawable = (TextureRegionDrawable) drawable;
            Texture texture = textureRegionDrawable.getRegion().getTexture();
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
    }

    // Метод для обновления ширины (если нужно)
    @Override
    public void setWidth(float width) {
        super.setWidth(width);// Пример: слайдер занимает 70% ширины
        updateButtonStyle();
    }

    // Геттер для текущего значения
    public float getValue() {
        return slider.getValue();
    }

    public void setValue(float initialQualityValue) {
        slider.setValue(initialQualityValue);
    }
}
