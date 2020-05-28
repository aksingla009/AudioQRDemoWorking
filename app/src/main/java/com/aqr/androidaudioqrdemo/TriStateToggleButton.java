package com.aqr.androidaudioqrdemo;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;
import com.facebook.rebound.SpringUtil;

import static com.aqr.androidaudioqrdemo.TriStateToggleButton.ToggleStatus.off;
import static com.aqr.androidaudioqrdemo.TriStateToggleButton.ToggleStatus.on;
import static com.aqr.androidaudioqrdemo.TriStateToggleButton.ToggleStatus.mid;


public class TriStateToggleButton extends View {
    //added a three state enumerator
    /*
    ON is to Receive
    OFF is to pay
    MID is for default neither pay nor receive
     */
    public enum ToggleStatus {
        on, mid, off
    }

    // convert from the string values defined in the xml attributes to an usable value
    private ToggleStatus attrToStatus(String attr) {
        if (attr == null) return off;
        if (attr.equals("0")) return off;
        else if (attr.equals("1")) return mid;
        else return on;
    }

    //static shortcuts for handling boolean values with 2-state. mid = false.
    public static boolean toggleStatusToBoolean(ToggleStatus toggleStatus) {
        if (toggleStatus == on) return true;
        else return false;
    }

    public static ToggleStatus booleanToToggleStatus(boolean toggleStatus) {
        if (toggleStatus) return on;
        else return off;
    }

    // same with integers
    public static int toggleStatusToInt(ToggleStatus toggleStatus) {
        switch (toggleStatus) {
            case off:
                return 0;
            case mid:
                return 1;
            case on:
            default:
                return 2;
        }
    }

    public static ToggleStatus intToToggleStatus(int toggleIntValue) {
        if (toggleIntValue == 0) return off;
        else if (toggleIntValue == 1) return mid;
        else return on;
    }


    private Spring spring;
    /**
     * radius
     */
    private float radius;
    // Turn on color
    private int onColor = Color.parseColor("#274FCC");  // green 300
    // Turn off border color
    private int offBorderColor = Color.parseColor("#274FCC");   // grey 400
    // Off color
    private int offColor = Color.parseColor("#274FCC");

    // third mid color
    private int midColor = Color.parseColor("#274FCC");  // amber 400

    //Spot is the circle which will be used as a selector in a switch
    private int spotColor = Color.parseColor("#40AEEE");
    // Border color
    private int borderColor = offBorderColor;
    // brush
    private Paint paint;
    // switch status
    private ToggleStatus toggleStatus = off;
    // added previousToggleStatus to manage transitions correctly
    private ToggleStatus previousToggleStatus = off;
    // Border size
    private int borderWidth = 2;
    // Vertical center
    private float centerY;
    // The start and end positions of the button
    // added midX position
    private float startX, midX, endX;
    // The minimum and maximum values for the X position of the handle
    //added spotMidX
    private float spotMinX, spotMidX, spotMaxX;
    // Handle size of inner circle
    private int spotSize;
    // Handle X position
    private float spotX;
    // Off Internal gray band height
    private float offLineWidth;

    private RectF rect = new RectF();
    // Animation is used by default
    private boolean defaultAnimate = true;
    // added midSelectable
    private boolean midSelectable = true;

    // swipe management
    private int swipeSensitivityPixels = 200;
    private int swipeX = 0;

    // default status of switch whether on off or mid
    private ToggleStatus defaultStatus = mid;
    // enabled && disabledColor
    private boolean enabled = true;
    private int disabledColor = Color.parseColor("#bdbdbd");   // grey 400

    private boolean swiping = false;

    private OnToggleChanged listener;

    private TriStateToggleButton(Context context) {
        super(context);
    }

    public TriStateToggleButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup(attrs);
    }

    public TriStateToggleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(attrs);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        spring.removeListener(springListener);
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        spring.addListener(springListener);
    }

    //Class variables to handle text strings of PAY , RECEIVE, and default state
    String textPay = "PAY";
    String textReceive = "RECEIVE";
    int text_width_pay;
    int text_height_pay;

    int text_width_receive;
    int text_height_receive;
    Paint paintForText;

    public void setup(AttributeSet attrs) {

        //For Text
        paintForText = new Paint();
        paintForText.setTypeface(Typeface.DEFAULT);// your preference here
        paintForText.setColor(Color.WHITE);
        paintForText.setTextSize(30);

        Rect boundsPay = new Rect();
        Rect boundsReceive = new Rect();
        paintForText.getTextBounds(textPay, 0, textPay.length(), boundsPay);
        paintForText.getTextBounds(textReceive, 0, textReceive.length(), boundsReceive);
        text_width_pay = boundsPay.width();
        text_height_pay = boundsPay.height();
        text_width_receive = boundsReceive.width();
        text_height_receive = boundsReceive.height();


        //Paint for layout
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.ROUND);

        SpringSystem springSystem = SpringSystem.create();
        spring = springSystem.createSpring();
        spring.setSpringConfig(SpringConfig.fromOrigamiTensionAndFriction(50, 7));

        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                //toggle(defaultAnimate);
            }
        });

        // Swipe management
        this.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int x = (int) motionEvent.getX();
                int action = motionEvent.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    swipeX = x;
                    swiping = false;
                } else if (action == MotionEvent.ACTION_MOVE) {
                    if (swipeSensitivityPixels == 0) return false;
                    else if (x - swipeX > swipeSensitivityPixels) {
                        swipeX = x;
                        swiping = true;
                        increaseValue();
                        return true;
                    } else if (swipeX - x > swipeSensitivityPixels) {
                        swipeX = x;
                        swiping = true;
                        decreaseValue();
                        return true;
                    }
                } else if (action == MotionEvent.ACTION_UP) {
                    //if (!swiping) toggle(defaultAnimate);    // here simple clicks are managed.
                    return true;
                }
                return false;
            }
        });

        switch (defaultStatus) {
            case off:
                toggleOff();
                break;  // actually not needed, added for clearness
            case mid:
                toggleMid();
                break;
            case on:
                toggleOn();
                break;
        }
    }

    // Iterate on the 3 values instead of switching between two
    public void toggle(boolean animate) {
        if (midSelectable)
            switch (toggleStatus) {
                case off:
                    putValueInToggleStatus(mid);
                    break;
                case mid:
                    putValueInToggleStatus(on);
                    break;
                case on:
                    putValueInToggleStatus(off);
                    break;
            }
        else
            switch (toggleStatus) {
                case off:
                case mid:
                    putValueInToggleStatus(on);
                    break;
                case on:
                    putValueInToggleStatus(off);
                    break;
            }
        takeEffect(animate);

        if (listener != null) {
            listener.onToggle(toggleStatus, toggleStatusToBoolean(toggleStatus), toggleStatusToInt(toggleStatus));
        }
    }

    public void toggleOn() {
        setToggleOn();
        if (listener != null) {
            listener.onToggle(toggleStatus, toggleStatusToBoolean(toggleStatus), toggleStatusToInt(toggleStatus));
        }
    }

    public void toggleOff() {
        setToggleOff();
        if (listener != null) {
            listener.onToggle(toggleStatus, toggleStatusToBoolean(toggleStatus), toggleStatusToInt(toggleStatus));
        }
    }

    // method to handle the mid value
    public void toggleMid() {
        setToggleMid();
        if (listener != null) {
            listener.onToggle(toggleStatus, toggleStatusToBoolean(toggleStatus), toggleStatusToInt(toggleStatus));
        }
    }

    private void putValueInToggleStatus(ToggleStatus value) {
        if (!enabled) return;
        previousToggleStatus = toggleStatus;
        toggleStatus = value;
    }

    // Setting the display to open style does not fire the toggle event
    public void setToggleOn() {
        setToggleOn(true);
    }

    /**
     * @param animate asd
     */
    public void setToggleOn(boolean animate) {
        putValueInToggleStatus(on);
        takeEffect(animate);
    }

    // Settings are shown as off styles, and the toggle event is not fired
    public void setToggleOff() {
        setToggleOff(true);
    }

    public void setToggleOff(boolean animate) {
        putValueInToggleStatus(off);
        takeEffect(animate);
    }

    // method for Mid value management
    public void setToggleMid(boolean animate) {
        putValueInToggleStatus(mid);
        takeEffect(animate);
    }

    public void setToggleMid() {
        setToggleMid(true);
    }

    //setToggleStatus() method, that imho was missing and needed
    public void setToggleStatus(ToggleStatus toggleStatus, boolean animate) {
        putValueInToggleStatus(toggleStatus);
        takeEffect(animate);
    }

    public void setToggleStatus(ToggleStatus toggleStatus) {
        setToggleStatus(toggleStatus, true);
    }

    public void setToggleStatus(boolean toggleStatus) {
        setToggleStatus(toggleStatus, true);
    }

    public void setToggleStatus(boolean toggleStatus, boolean animate) {
        if (toggleStatus) putValueInToggleStatus(on);
        else putValueInToggleStatus(off);
        takeEffect(animate);
    }

    public void setToggleStatus(int toggleIntValue) {
        setToggleStatus(toggleIntValue, true);
    }

    public void setToggleStatus(int toggleIntValue, boolean animate) {
        setToggleStatus(intToToggleStatus(toggleIntValue), animate);
    }

    public void increaseValue(boolean animate) {  // same as toggle, but after on does not rewind to off
        switch (toggleStatus) {
            case off:
                if (midSelectable) putValueInToggleStatus(mid);
                else putValueInToggleStatus(on);
                break;
            case mid:
                putValueInToggleStatus(on);
                break;
            case on:
                break;
        }
        takeEffect(animate);
        if (listener != null) {
            listener.onToggle(toggleStatus, toggleStatusToBoolean(toggleStatus), toggleStatusToInt(toggleStatus));
        }
    }

    public void increaseValue() {
        increaseValue(true);
    }

    public void decreaseValue(boolean animate) {
        switch (toggleStatus) {
            case on:
                if (midSelectable) putValueInToggleStatus(mid);
                else putValueInToggleStatus(off);
                break;
            case mid:
                putValueInToggleStatus(off);
                break;
            case off:
                break;
        }
        takeEffect(animate);
        if (listener != null) {
            listener.onToggle(toggleStatus, toggleStatusToBoolean(toggleStatus), toggleStatusToInt(toggleStatus));
        }
    }

    public void decreaseValue() {
        decreaseValue(true);
    }

    private void takeEffect(boolean animate) {
        if (animate) {
            spring.setEndValue(toggleStatus == on ? 1 : toggleStatus == off ? 0 : 0.5);
        } else {
            // There is no call spring, so the current value of the spring has not changed, here to set it, the current value on both sides of synchronization
            spring.setCurrentValue(toggleStatus == on ? 1 : toggleStatus == off ? 0 : 0.5);
            if (toggleStatus == on) calculateEffect(1);
            else if (toggleStatus == mid) calculateEffect(0.5);
            else calculateEffect(0);
        }
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        Resources r = Resources.getSystem();
        if (widthMode == MeasureSpec.UNSPECIFIED || widthMode == MeasureSpec.AT_MOST) {
            widthSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, r.getDisplayMetrics());
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
        }

        if (heightMode == MeasureSpec.UNSPECIFIED || heightSize == MeasureSpec.AT_MOST) {
            heightSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, r.getDisplayMetrics());
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
        }


        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        final int width = getWidth();
        final int height = getHeight();

        radius = Math.min(width, height) * 0.5f;
        centerY = radius;
        startX = radius;
        endX = width - radius;
        spotMinX = startX + borderWidth;
        spotMaxX = endX - borderWidth;
        spotMidX = (startX + endX) / 2;
        spotSize = height - 20 * borderWidth;
        //management of the position according to 3 states
        spotX = toggleStatus == on ? spotMaxX : toggleStatus == off ? spotMinX : spotMidX;
        offLineWidth = 0;
    }


    SimpleSpringListener springListener = new SimpleSpringListener() {
        @Override
        public void onSpringUpdate(Spring spring) {
            final double value = spring.getCurrentValue();
            calculateEffect(value);
        }
    };

    private int clamp(int value, int low, int high) {
        return Math.min(Math.max(value, low), high);
    }


    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        //outer rectangle
        rect.set(0, 0, getWidth(), getHeight());
        paint.setColor(borderColor);
        canvas.drawRoundRect(rect, radius, radius, paint);

        //Rectangle if Outer border then just create round rectange of outer border
        if (offLineWidth > 0) {
            final float cy = offLineWidth * 0.5f;
            rect.set(spotX - cy, centerY - cy, endX + cy, centerY + cy);
            paint.setColor(enabled ? (toggleStatus == mid ? midColor : offColor) : disabledColor);
            canvas.drawRoundRect(rect, cy, cy, paint);
        }

        //Rectangle with bigger inner rectangle colored filled
        rect.set(spotX - radius, centerY - radius, spotX + radius, centerY + radius);
        paint.setColor(enabled ? borderColor : disabledColor);
        canvas.drawRoundRect(rect, radius, radius, paint);

        //spot
        final float spotR = spotSize * 0.5f;
        rect.set(spotX - spotR, centerY - spotR, spotX + spotR, centerY + spotR);
        paint.setColor(enabled ? spotColor : disabledColor);
        canvas.drawRoundRect(rect, spotR, spotR, paint);

        if (toggleStatus == off) {
            canvas.drawText(textPay, spotX - ((float) text_width_pay / 2), centerY + ((float) text_height_pay / 2), paintForText);
        } else if (toggleStatus == on) {
            canvas.drawText(textReceive, spotX - ((float) text_width_receive / 2), centerY + ((float) text_height_receive / 2), paintForText);
        } else {
            canvas.drawText(textPay, startX - ((float) text_width_pay / 2), centerY + ((float) text_height_pay / 2), paintForText);
            canvas.drawText(textReceive, endX - ((float) text_width_receive / 2), centerY + ((float) text_height_receive / 2), paintForText);
        }


    }

    private void calculateEffect(final double value) {
        spotX = (float) SpringUtil.mapValueFromRangeToRange(value, 0, 1, spotMinX, spotMaxX);
        double min = 0, max = 0;
        int fromColor, toColor;
        if (previousToggleStatus == off && toggleStatus == mid) {
            toColor = offBorderColor;
            fromColor = midColor;
        } else if (previousToggleStatus == off && toggleStatus == on) {
            toColor = offBorderColor;
            fromColor = onColor;
        } else if (previousToggleStatus == mid && toggleStatus == on) {
            toColor = midColor;
            fromColor = onColor;
        } else if (previousToggleStatus == on && toggleStatus == off) {
            toColor = offBorderColor;
            fromColor = onColor;
        } else if (previousToggleStatus == on && toggleStatus == mid) {
            toColor = midColor;
            fromColor = onColor;
        } else {
            toColor = offBorderColor;
            fromColor = onColor;
        }

        if (previousToggleStatus == off) min = 0;
        else if (previousToggleStatus == mid) min = 0.5;
        else min = 1;
        if (toggleStatus == off) max = 0;
        else if (toggleStatus == mid) max = 0.5;
        else max = 1;

        if (min == max) {
            min = 0;
            max = 1;
        } else if (min > max) {
            double temp = min;
            min = max;
            max = temp;
        }

        offLineWidth = (float) SpringUtil.mapValueFromRangeToRange(min + max - value, min, max, 0, spotSize);

        final int fromB = Color.blue(fromColor);
        final int fromR = Color.red(fromColor);
        final int fromG = Color.green(fromColor);
        final int toB = Color.blue(toColor);
        final int toR = Color.red(toColor);
        final int toG = Color.green(toColor);

        int springB = (int) SpringUtil.mapValueFromRangeToRange(min + max - value, min, max, fromB, toB);
        int springR = (int) SpringUtil.mapValueFromRangeToRange(min + max - value, min, max, fromR, toR);
        int springG = (int) SpringUtil.mapValueFromRangeToRange(min + max - value, min, max, fromG, toG);

        springB = clamp(springB, 0, 255);
        springR = clamp(springR, 0, 255);
        springG = clamp(springG, 0, 255);

        borderColor = Color.rgb(springR, springG, springB);

        postInvalidate();
    }

    public interface OnToggleChanged {
        void onToggle(ToggleStatus toggleStatus, boolean booleanToggleStatus, int toggleIntValue);
    }

    public void setOnToggleChanged(OnToggleChanged onToggleChanged) {
        listener = onToggleChanged;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        postInvalidate();
        super.setEnabled(enabled);
    }
}