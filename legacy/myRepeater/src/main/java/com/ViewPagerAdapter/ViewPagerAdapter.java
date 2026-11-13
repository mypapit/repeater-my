package com.ViewPagerAdapter;

import android.content.Context;
import android.graphics.Matrix;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.mypapit.mobile.myrepeater.R;
import net.mypapit.mobile.myrepeater.WalkthroughActivity;

public class ViewPagerAdapter extends android.support.v4.view.PagerAdapter {

    WalkthroughActivity context;
    String[] title;
    String[] description;
    int[] icon;
    boolean[] displayButton;
    LayoutInflater inflater;

    public ViewPagerAdapter(WalkthroughActivity context, String[] title, String[] description, int[] icon,
                            boolean[] displayButton) {

        this.context = context;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.displayButton = displayButton;

    }

    public Object instantiateItem(ViewGroup container, int position) {

        TextView txttitle;
        TextView txtdescription;
        ImageView imgflag;
        Button btnDismiss;

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View itemView = inflater.inflate(R.layout.viewpager_tutorial, container, false);

        txttitle = (TextView) itemView.findViewById(R.id.tvTitleTutorial);
        txtdescription = (TextView) itemView.findViewById(R.id.tvDescriptionTutorial);
        btnDismiss = (Button) itemView.findViewById(R.id.btnDismissTutorial);
        if (!displayButton[position]) {
            btnDismiss.setVisibility(View.GONE);
        }

        imgflag = (ImageView) itemView.findViewById(R.id.ivImageTutorial);

        txttitle.setText(title[position]);
        txtdescription.setText(description[position]);
        imgflag.setImageResource(icon[position]);

        if (icon[position] == R.drawable.arrow) {
            Matrix matrix = new Matrix();
            imgflag.setScaleType(ScaleType.MATRIX);
            matrix.postRotate(45.0f);
            imgflag.setImageMatrix(matrix);


        } else {

            Animation bounceAnimation = AnimationUtils.loadAnimation(context, R.anim.bounceanim);
            imgflag.setAnimation(bounceAnimation);
        }


        container.addView(itemView);

        btnDismiss.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                context.finish();


            }


        });

        return itemView;


    }

    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((RelativeLayout) object);

    }

    @Override
    public int getCount() {

        return title.length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {

        return view == object;
    }

}
