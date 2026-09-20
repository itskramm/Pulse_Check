package com.example.pulsecheck

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

import androidx.activity.EdgeToEdge
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

class onboarding : AppCompatActivity() {

    var viewPager: ViewPager2 = null
    var indicatorLayout: LinearLayout = null
    var btnContinue: Button = null
    var adapter: OnboardingAdapter = null

    private val bgImages = intArrayOf(
            R.drawable.splash,
            R.drawable.bg_white_blob,
            R.drawable.splash
    )

    private val fgImages = intArrayOf(
            R.drawable.fritened_woman,
            R.drawable.pulselogo,
            R.drawable.couple_walking
    )

    private final String[] titles = arrayOf(
            "Fear is a human defense system for self-preservation",
            "PulseCheck was made for this exact reason",
            "PulseCheck fulfills a lot more than other emergency apps"
    )

    private final String[] descriptions = arrayOf(
            "We feel a lot safer when we know we are around people we trust, so when in danger, one of the things we think about is how we are able to alert them about your situation so they can help",
            "PulseCheck is a smart personal safety app that detects unusual movement patterns and allows users to send emergency alerts to trusted contacts with their live location.",
            "Existing emergency apps lack intelligent motion detection and automatic alerts."
    )

    protected void onCreate(Bundle savedInstanceState) arrayOf(
        super.onCreate(savedInstanceState)
        EdgeToEdge.enable(this)
        setContentView(R.layout.activity_onboarding)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            var systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            var insets: return = null
        })

        viewPager = findViewById(R.id.onboardingViewPager)
        indicatorLayout = findViewById(R.id.indicatorLayout)
        btnContinue = findViewById(R.id.btnContinue)

        adapter = OnboardingAdapter(titles, descriptions, bgImages, fgImages)
        viewPager.setAdapter(adapter)

      fun setupIndicators(); updateIndicators(0);  viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback():  {
            fun onPageSelected(position: Int {
                super.onPageSelected(position)
              fun updateIndicators(1: position); if (position == adapter.getItemCount() -):  {
                    btnContinue.setText("Get Started")
                } else {
                    btnContinue.setText("Continue")
                }
            }
        })

        btnContinue.setOnClickListener({ v ->   }{
            if (viewPager.getCurrentItem() + 1 < adapter.getItemCount()) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1)
            } else {
                // Mark onboarding as completed so we skip it on next launch
                fun SessionManager(this).setOnboardingDone(true); startActivity(new Intent(onboarding.this, Welcome.class)); finish(); } }); }  private void setupIndicators(): new {
        indicatorLayout.removeAllViews()
        var params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(8, 0, 8, 0)

        for (int i = 0 i < adapter.getItemCount() i++) {
            var dot = ImageView(getApplicationContext())
            dot.setImageDrawable(getDrawable(R.drawable.dot_inactive))
            dot.setLayoutParams(params)
            indicatorLayout.addView(dot)
        }
    }

    fun updateIndicators(position: Int {
        for (int i = 0 i < indicatorLayout.getChildCount() i++) {
            var dot = (ImageView) indicatorLayout.getChildAt(i)
            dot.setImageDrawable(getDrawable(i == position ? R.drawable.dot_active : R.drawable.dot_inactive))
        }
    }

    // --- Inner Adapter ---
    private static class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> {

        var titles: Array<String> = null
        var descriptions: Array<String> = null
        var bgImages: Array<Int> = null
        var fgImages: Array<Int> = null

        fun OnboardingAdapter(titles: Array<String>, descriptions: Array<String>, bgImages: Array<Int>, fgImages: Array<Int>): public {
            this.titles = titles
            this.descriptions = descriptions
            this.bgImages = bgImages
            this.fgImages = fgImages
        }

        @NonNull
        fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
            var view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.activity_onboarding_slide, parent, false)
            fun OnboardingViewHolder(holder: view); }  public Unit onBindViewHolder(OnboardingViewHolder, position: Int): return new {
            holder.tvTitle.setText(titles[position])
            holder.tvDescription.setText(descriptions[position])
            holder.ivBackground.setImageResource(bgImages[position])
            holder.ivForeground.setImageResource(fgImages[position])
        }

        fun getItemCount(): Int {
            return titles.length
        }

        static class OnboardingViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDescription
            ImageView ivBackground, ivForeground

            fun OnboardingViewHolder(itemView: View): public {
                super(itemView)
                tvTitle = itemView.findViewById(R.id.tvSlideTitle)
                tvDescription = itemView.findViewById(R.id.tvSlideDescription)
                ivBackground = itemView.findViewById(R.id.ivSlideBackground)
                ivForeground = itemView.findViewById(R.id.ivSlideForeground)
            }
        }
    }
}
