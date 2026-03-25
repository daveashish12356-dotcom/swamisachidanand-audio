package com.swamisachidanand;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import android.content.res.ColorStateList;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

/**
 * Interactive tour: opens real tabs, spotlights main UI (lists, search, quick nav, bottom bar)
 * with detailed Gujarati hints. Content highlight is tried first, then bottom-nav item.
 */
public class InteractiveAppTourHelper {

    private static final String TAG = "InteractiveTour";

    private final MainActivity activity;
    @Nullable
    private ViewGroup overlay;
    @Nullable
    private TourSpotlightView spotlight;
    @Nullable
    private TextView titleView;
    @Nullable
    private TextView messageView;
    @Nullable
    private TextView stepProgressView;
    @Nullable
    private MaterialButton btnNext;
    @Nullable
    private MaterialButton btnSkip;

    private int stepIndex = 0;
    /** Cancels stale spotlight updates when user taps Next quickly. */
    private int spotlightSeq = 0;

    private static final class Step {
        @IdRes final int tabId;
        /** Bottom-nav menu item to ring (0 = none). */
        @IdRes final int highlightMenuId;
        /** Main screen widget (recycler, card, bottom bar…). 0 = none. */
        @IdRes final int contentAnchorId;
        final boolean showBottomNav;
        /** Smooth-scroll Home NestedScrollView so anchor is visible before spotlight. */
        final boolean autoScrollHomeToAnchor;
        @NonNull final String title;
        @NonNull final String message;

        Step(int tabId, int highlightMenuId, int contentAnchorId, boolean showBottomNav,
             boolean autoScrollHomeToAnchor,
             @NonNull String title, @NonNull String message) {
            this.tabId = tabId;
            this.highlightMenuId = highlightMenuId;
            this.contentAnchorId = contentAnchorId;
            this.showBottomNav = showBottomNav;
            this.autoScrollHomeToAnchor = autoScrollHomeToAnchor;
            this.title = title;
            this.message = message;
        }
    }

    /**
     * Home first: top bar → Swami avatar → shortcuts → book store (auto-scroll) → hero/suvichar → other tabs…
     */
    private final Step[] steps = new Step[]{
            new Step(0, 0, R.id.home_swagat_header_box, false, true,
                    "🏠 હોમ — ઉપરનું બાર (નેવિગેશન)",
                    "આ ડિબ્બામાં બધું એકસાથે છે:\n\n"
                            + "• \"हरिः ॐ सुस्वागतम्\" સ્વાગત લાઇન\n"
                            + "• જમણી બાજુ સ્વામીજીનું નાનું ફોટો — આગળના પગલામાં તેની વિગત\n"
                            + "• નીચે શોધ બાર: લખીને પુસ્તક/ઓડિયો શોધો; માઇકથી વૉઇસ શોધ પણ ચાલે\n\n"
                            + "એપ નીચે સ્ક્રોલ કરશો તો બીજા સેક્શન દેખાશે — ટૂર માટે આપણે આપમેળે સ્ક્રોલ પણ કરીશું."),
            new Step(0, 0, R.id.global_profile_avatar, false, true,
                    "🙏 સ્વામીજી — પ્રોફાઇલ પેજ",
                    "આ ગોળ ફોટા પર ટેપ કરશો તો સ્વામીજીની માહિતી પેજ ખુલશે (જીવન પરિચય, ફોટા વગેરે).\n\n"
                            + "પાછા આવવા માટે ફોનનું બેક બટન વાપરો."),
            new Step(0, 0, R.id.home_quick_nav_card, false, true,
                    "⚡ ઝડપી શોર્ટકટ્સ",
                    "આ કાર્ડમાં પાંચ બટન છે: પુસ્તક, ઓડિયો, પ્રવચન, વિડિઓ અને સંપર્ક — એક ટેપથી સીધું તે ટેબ ખુલશે.\n\n"
                            + "હવે નીચે બુક સ્ટોર અને બીજા હોમ સેક્શન, પછી દરેક મુખ્ય પેજ બતાવીશું."),
            new Step(0, 0, R.id.home_book_store_recycler, false, true,
                    "📖 પુસ્તકાલય / બુક સ્ટોર",
                    "આ લીસ્ટમાં ઓનલાઇન બુક સ્ટોરના કાર્ડો છે — શીર્ષક અને કિંમત.\n\n"
                            + "ઉપર \"સચ્ચિદાનંદજી પુસ્તકાલય\" અને View More › ટેપ કરી પૂરું પેજ ખુલશે. સંપર્ક પેજ પર પણ ઓર્ડર માહિતી છે.\n\n"
                            + "(સેક્શન નીચે હોવાથી એપ આપમેળે સ્ક્રોલ કરશે — થોડી સેકંડ રાહ જુઓ.)"),
            new Step(0, 0, R.id.hero_video_container, false, true,
                    "🏠 સુવિચાર અને હીરો વિડિઓ",
                    "આ બોક્સમાં સુવિચાર ઓવરલે અને હીરો વિડિઓ/ફોટો ચાલે છે.\n\n"
                            + "નીચે સ્ક્રોલ કરશો તો \"છેલ્લું વાંચ્યું/સાંભળ્યું\", શ્રેષ્ઠ પુસ્તકો અને \"આજનું પ્રવચન\" જેવા સેક્શન મળશે. જાહેરાત બેનર એપ સપોર્ટ માટે છે."),
            new Step(R.id.nav_books, R.id.nav_books, R.id.category_chips_recycler, true, false,
                    "📚 પુસ્તક — શ્રેણી અને PDF",
                    "ઉપર ચિપ્સથી શ્રેણી પસંદ કરો (જેમ કે નવા, ભક્તિ…). નીચે સેક્શન પ્રમાણે પુસ્તકો ગોઠવાયેલા છે.\n\n"
                            + "પુસ્તક પર ટેપ કરશો તો એપમાં જ PDF રીડર ખુલશે. શોધ બારથી નામથી શોધો.\n\n"
                            + "\"પુસ્તક મંગાવવા\" લિંકથી ઓર્ડર માહિતી મળશે."),
            new Step(R.id.nav_audio, R.id.nav_audio, R.id.audio_sections_recycler, true, false,
                    "🎧 ઓડિયોબુક — ભાગો અને પ્લેયર",
                    "ઓડિયો પુસ્તકો વિભાગ પ્રમાણે લિસ્ટ થયેલા છે. શોધથી ઝડપથી મળી જશે.\n\n"
                            + "એક પુસ્તક પસંદ કરશો તો વિગત પેજ અને પ્લેયર ખુલશે. ઓડિયો ચાલુ હોય તો ક્યારેક નીચે મિની પ્લેયર પટ્ટી પણ દેખાશે — ત્યાંથી ચાલુ/થંભાવી શકાય."),
            new Step(R.id.nav_pravachan, R.id.nav_pravachan, R.id.pravachan_recycler, true, false,
                    "🔔 પ્રવચન — દૈનિક ઓડિયો",
                    "અહીં દૈનિક પ્રવચનોની યાદી છે; શોધથી તારીખ અથવા શીર્ષક મુજબ શોધી શકાય.\n\n"
                            + "લાઇન પર ટેપ કરી સીધું સાંભળી શકાય. નવું પ્રવચન આવે ત્યારે (જો નોટિફિકેશન પરવાનગી આપી હોય) એલર્ટ મળી શકે.\n\n"
                            + "હોમ પર \"આજનું પ્રવચન\" કાર્ડથી પણ અહીં ઝડપથી પહોંચી શકાય."),
            new Step(R.id.nav_videos, R.id.nav_videos, R.id.videos_recycler, true, false,
                    "🎬 વિડિઓ — YouTube લિસ્ટ",
                    "અહીં લાંબા વિડિઓ અને શોર્ટ્સ એક જ લિસ્ટમાં છે; ટેપ કરી યુટ્યુબ પ્લેયર ખુલશે.\n\n"
                            + "ઉપર રિફ્રેશ (swipe) કરી નવી લિસ્ટ લાવી શકાય. ક્યારેક નવી ટોચની વિડિઓ માટે સ્થાનિક સૂચના પણ આવી શકે."),
            new Step(R.id.nav_about, R.id.nav_about, R.id.sampark_first_photo_card, true, false,
                    "📞 સંપર્ક — સોશિયલ અને માહિતી",
                    "ફોટો, સ્લાઇડશો, યુટ્યુબ, ફેસબુક, ઇન્સ્ટા, વોટ્સએપ, ટેલીગ્રામ — બધી લિંક્સ અહીં છે.\n\n"
                            + "ફોટો ગેલેરી, પુસ્તક ઓર્ડર/સંપર્ક કાર્ડ, આશ્રમ સૂચના અને \"એપ ટૂર ફરી જુઓ\" બટન પણ આ જ પેજ પર છે."),
            new Step(R.id.nav_books, 0, R.id.bottom_navigation, true, false,
                    "📍 નીચેનું મેનુ (મહત્વનું)",
                    "છે છ વસ્તુઓ: હોમ · પુસ્તક · ઓડિયો · પ્રવચન · વિડિઓ · સંપર્ક.\n\n"
                            + "જે ટેબ પર છો તેનું નામ અહીં દેખાય (લેબલ). હોમ પર નીચેનું મેનુ થોડી વાર સ્ક્રોલ કરતા છુપાઈ જાય — ફરી ઉપર સ્ક્રોલ કરો અથવા બીજો ટેબ ખોલો તો પાછું દેખાશે."),
            new Step(R.id.nav_home, 0, R.id.hero_video_container, false, true,
                    "🏠 ફરી હોમ",
                    "તમે હવે બધા મુખ્ય પેજ જોઈ લીધા.\n\n"
                            + "બેક બટનથી પહેલાં ટેબ પર પાછા જઈ શકાય (જ્યાં સુધી ઇતિહાસ હોય). વિગત પેજ (જેમ PDF/ઓડિયો વિગત) ખુલ્યું હોય તો પહેલા તે બંધ થશે.\n\n"
                            + "કોઈ મુશ્કેલી હોય તો સંપર્ક પેજ પર \"એપ ટૂર ફરી જુઓ\" દબાવો."),
            new Step(0, 0, 0, false, false,
                    "✅ તૈયાર — આનંદથી ઉપયોગ કરો",
                    "આ ટૂર પૂરો થયો. સ્વામીજીના પ્રવચન, પુસ્તક, ઓડિયો અને વિડિઓ નિયમિત મળતા રહે તે માટે એપ ખુલ્લી રાખજો.\n\n"
                            + "શુભમ ભવતુ 🙏"),
    };

    public InteractiveAppTourHelper(@NonNull MainActivity activity) {
        this.activity = activity;
    }

    public void start() {
        stepIndex = 0;
        attachOverlay();
        applyStep(false);
    }

    public boolean isShowing() {
        return overlay != null && overlay.getParent() != null;
    }

    /** Back = skip tour (still mark as seen so Splash won’t show tour every launch). */
    public boolean onBackPressed() {
        if (!isShowing()) return false;
        dismiss();
        return true;
    }

    private void attachOverlay() {
        removeOverlay();
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null) return;
        overlay = (ViewGroup) LayoutInflater.from(activity).inflate(R.layout.layout_interactive_tour_overlay, content, false);
        spotlight = overlay.findViewById(R.id.tour_spotlight);
        titleView = overlay.findViewById(R.id.tour_card_title);
        stepProgressView = overlay.findViewById(R.id.tour_step_progress);
        messageView = overlay.findViewById(R.id.tour_card_message);
        btnNext = overlay.findViewById(R.id.tour_btn_next);
        btnSkip = overlay.findViewById(R.id.tour_btn_skip);

        // Theme can wash out M3 button colors — force readable contrast on all devices.
        try {
            if (btnNext != null) {
                int fill = ContextCompat.getColor(activity, R.color.bhagva_dark);
                btnNext.setBackgroundTintList(ColorStateList.valueOf(fill));
                btnNext.setTextColor(ContextCompat.getColor(activity, android.R.color.white));
            }
            if (btnSkip != null) {
                btnSkip.setTextColor(ContextCompat.getColor(activity, R.color.bhagva_dark));
            }
        } catch (Throwable ignored) {
        }

        if (btnSkip != null) {
            btnSkip.setOnClickListener(v -> dismiss());
        }
        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                if (stepIndex >= steps.length - 1) {
                    dismiss();
                } else {
                    stepIndex++;
                    applyStep(true);
                }
            });
        }

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        content.addView(overlay, lp);
    }

    private void applyStep(boolean animateNav) {
        if (stepIndex < 0 || stepIndex >= steps.length) return;
        Step s = steps[stepIndex];

        activity.setSuppressTabBackStackPushForTour(true);
        try {
            if (s.showBottomNav) {
                activity.setBottomNavVisibleForTour(true);
            } else {
                activity.setBottomNavVisibleForTour(false);
            }
            if (activity.getBottomNavigation() != null) {
                if (s.tabId != 0) {
                    activity.getBottomNavigation().setSelectedItemId(s.tabId);
                } else {
                    activity.getBottomNavigation().setSelectedItemId(R.id.nav_home);
                }
            }
        } finally {
            activity.setSuppressTabBackStackPushForTour(false);
        }

        if (titleView != null) titleView.setText(s.title);
        if (messageView != null) messageView.setText(s.message);
        if (stepProgressView != null) {
            stepProgressView.setVisibility(View.VISIBLE);
            stepProgressView.setText(activity.getString(R.string.tour_step_format, stepIndex + 1, steps.length));
        }
        if (btnNext != null) {
            btnNext.setText(stepIndex >= steps.length - 1
                    ? activity.getString(R.string.tour_done)
                    : activity.getString(R.string.tour_next));
        }

        final Runnable scheduleSpotlight = () -> {
            spotlightSeq++;
            final int token = spotlightSeq;
            View decor = activity.getWindow().getDecorView();
            long delay1 = animateNav ? 520 : 120;
            decor.postDelayed(() -> updateSpotlightHole(token), delay1);
            if (animateNav) {
                decor.postDelayed(() -> updateSpotlightHole(token), 1150);
            }
        };

        boolean onHomeTab = s.tabId == 0 || s.tabId == R.id.nav_home;
        if (s.autoScrollHomeToAnchor && onHomeTab && s.contentAnchorId != 0) {
            View decor = activity.getWindow().getDecorView();
            decor.post(() -> scrollHomeNestedScrollToAnchor(s.contentAnchorId, this::scheduleHomeSpotlightsAfterScroll));
        } else {
            scheduleSpotlight.run();
        }
    }

    /** Home scroll needs longer settle + multiple spotlight passes (layout still settling). */
    private void scheduleHomeSpotlightsAfterScroll() {
        spotlightSeq++;
        final int token = spotlightSeq;
        View decor = activity.getWindow().getDecorView();
        decor.postDelayed(() -> updateSpotlightHole(token), 420);
        decor.postDelayed(() -> updateSpotlightHole(token), 980);
        decor.postDelayed(() -> updateSpotlightHole(token), 1700);
        decor.postDelayed(() -> updateSpotlightHole(token), 2500);
        decor.postDelayed(() -> updateSpotlightHole(token), 3300);
    }

    /**
     * Scroll home NestedScrollView until anchor is on screen.
     * Uses requestChildRectangleOnScreen + requestRectangleOnScreen + scrollY fallback; retries while layout loads.
     */
    private void scrollHomeNestedScrollToAnchor(@IdRes int anchorId, @NonNull Runnable then) {
        NestedScrollView nsv = activity.findViewById(R.id.home_nested_scroll);
        final ViewGroup content = (nsv != null && nsv.getChildCount() > 0) ? (ViewGroup) nsv.getChildAt(0) : null;
        if (nsv == null || content == null) {
            then.run();
            return;
        }
        final int pad = (int) (56f * activity.getResources().getDisplayMetrics().density);
        if (anchorId == R.id.home_book_store_recycler || anchorId == R.id.home_book_store_section) {
            nsv.post(() -> scrollHomeToBookStore(nsv, content, pad, then));
            return;
        }
        nsv.post(() -> attemptHomeScrollToAnchor(nsv, content, anchorId, pad, 0, then));
    }

    /**
     * Book store is far down; nested RecyclerViews change content height after layout.
     * Several instant {@code scrollTo} passes + nudge work better than one smooth scroll.
     */
    private void scrollHomeToBookStore(@NonNull NestedScrollView nsv, @NonNull ViewGroup content,
                                       int pad, @NonNull Runnable then) {
        long[] delaysMs = {60, 280, 550, 900, 1400, 2000, 2700, 3400};
        for (long d : delaysMs) {
            nsv.postDelayed(() -> applyScrollToBookStoreAnchor(nsv, content, pad), d);
        }
        nsv.postDelayed(() -> applyScrollToBookStoreAnchor(nsv, content, pad), 4100);
        nsv.postDelayed(then, 4300);
    }

    private void applyScrollToBookStoreAnchor(@NonNull NestedScrollView nsv, @NonNull ViewGroup content, int pad) {
        try {
            View anchor = activity.findViewById(R.id.home_book_store_recycler);
            if (anchor == null || !isDescendantOf(anchor, content) || anchor.getVisibility() != View.VISIBLE) {
                anchor = activity.findViewById(R.id.home_book_store_section);
            }
            int maxY = Math.max(0, content.getHeight() - nsv.getHeight());
            if (anchor != null && isDescendantOf(anchor, content) && anchor.getVisibility() == View.VISIBLE) {
                int y = computeOffsetTopInContent(anchor, content);
                int target = Math.min(maxY, Math.max(0, y - pad));
                nsv.scrollTo(0, target);
                nudgeHomeScrollY(nsv, content, anchor, pad);
                return;
            }
            if (maxY > 0) {
                float d = activity.getResources().getDisplayMetrics().density;
                int approx = (int) (880f * d);
                nsv.scrollTo(0, Math.min(maxY, Math.max(maxY * 8 / 10, approx)));
            }
        } catch (Throwable t) {
            Log.e(TAG, "applyScrollToBookStoreAnchor", t);
        }
    }

    /** Distance from top of {@code content} (NestedScrollView child) to top of {@code anchor}. */
    private static int computeOffsetTopInContent(@NonNull View anchor, @NonNull ViewGroup content) {
        int sum = 0;
        View v = anchor;
        while (v != null && v != content) {
            sum += v.getTop();
            ViewParent p = v.getParent();
            if (!(p instanceof View)) break;
            v = (View) p;
        }
        return sum;
    }

    private void attemptHomeScrollToAnchor(@NonNull NestedScrollView nsv, @NonNull ViewGroup content,
                                           @IdRes int anchorId, int pad, int attempt, @NonNull Runnable then) {
        if (attempt > 16) {
            nsv.postDelayed(then, 200);
            return;
        }
        try {
            View anchor = activity.findViewById(anchorId);
            boolean ok = anchor != null
                    && anchor.getVisibility() == View.VISIBLE
                    && isDescendantOf(anchor, content);

            if (!ok) {
                nsv.postDelayed(() -> attemptHomeScrollToAnchor(nsv, content, anchorId, pad, attempt + 1, then), 260);
                return;
            }

            Rect rect = new Rect();
            anchor.getDrawingRect(rect);
            content.offsetDescendantRectToMyCoords(anchor, rect);
            rect.top -= pad;
            rect.bottom += pad;
            rect.left -= pad;
            rect.right += pad;
            nsv.requestChildRectangleOnScreen(content, rect, false);

            nsv.post(() -> {
                try {
                    Rect childRect = new Rect(0, -pad, Math.max(1, anchor.getWidth()),
                            Math.max(1, anchor.getHeight() + pad));
                    anchor.requestRectangleOnScreen(childRect, false);
                } catch (Throwable ignored) {
                }
            });

            nudgeHomeScrollY(nsv, content, anchor, pad);

            nsv.postDelayed(() -> {
                nudgeHomeScrollY(nsv, content, activity.findViewById(anchorId), pad);
                nsv.postDelayed(then, 520);
            }, 380);
        } catch (Throwable t) {
            Log.e(TAG, "attemptHomeScrollToAnchor", t);
            nsv.postDelayed(then, 200);
        }
    }

    /** If anchor is still off-screen, adjust scrollY using screen coordinates. */
    private void nudgeHomeScrollY(@NonNull NestedScrollView nsv, @NonNull ViewGroup content,
                                  @Nullable View anchor, int pad) {
        if (anchor == null || anchor.getVisibility() != View.VISIBLE) return;
        try {
            int[] aLoc = new int[2];
            int[] nLoc = new int[2];
            anchor.getLocationOnScreen(aLoc);
            nsv.getLocationOnScreen(nLoc);
            int vTop = nLoc[1];
            int vBottom = vTop + nsv.getHeight();
            int aTop = aLoc[1];
            int aBottom = aTop + anchor.getHeight();
            int marginTop = vTop + pad + (int) (72f * activity.getResources().getDisplayMetrics().density);
            int marginBottom = vBottom - pad - (int) (100f * activity.getResources().getDisplayMetrics().density);
            int scrollY = nsv.getScrollY();
            if (aTop < marginTop) {
                int delta = marginTop - aTop;
                nsv.smoothScrollTo(0, Math.max(0, scrollY - delta));
            } else if (aBottom > marginBottom) {
                int delta = aBottom - marginBottom;
                int maxY = Math.max(0, content.getHeight() - nsv.getHeight());
                nsv.smoothScrollTo(0, Math.min(maxY, scrollY + delta));
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean isDescendantOf(@Nullable View view, @Nullable ViewGroup ancestor) {
        if (view == null || ancestor == null) return false;
        View v = view;
        while (v != null) {
            if (v == ancestor) return true;
            ViewParent p = v.getParent();
            if (!(p instanceof View)) return false;
            v = (View) p;
        }
        return false;
    }

    private void updateSpotlightHole(int token) {
        if (token != spotlightSeq || spotlight == null || !isShowing()) return;
        if (stepIndex < 0 || stepIndex >= steps.length) return;
        Step s = steps[stepIndex];
        int pad = (int) (10f * activity.getResources().getDisplayMetrics().density);

        if (s.contentAnchorId != 0) {
            View anchor = activity.findViewById(s.contentAnchorId);
            if (trySetHoleForView(anchor, pad)) {
                return;
            }
            if (s.contentAnchorId == R.id.home_book_store_recycler) {
                if (trySetHoleForView(activity.findViewById(R.id.home_book_store_section), pad)) {
                    return;
                }
                if (trySetHoleForView(activity.findViewById(R.id.home_book_store_view_all), pad)) {
                    return;
                }
            }
        }

        if (s.highlightMenuId != 0 && s.showBottomNav) {
            BottomNavigationView nav = activity.getBottomNavigation();
            if (nav != null && nav.getVisibility() == View.VISIBLE) {
                View item = findBottomNavItemView(nav, s.highlightMenuId);
                if (item != null && trySetHoleForView(item, pad)) {
                    return;
                }
            }
        }

        spotlight.setHoleRect(null, pad);
    }

    private boolean trySetHoleForView(@Nullable View v, int padPx) {
        if (v == null || spotlight == null) return false;
        if (!v.isAttachedToWindow()) return false;
        if (v.getVisibility() != View.VISIBLE) return false;
        Rect r = new Rect();
        if (!v.getGlobalVisibleRect(r) || r.isEmpty()) return false;
        int[] overlayLoc = new int[2];
        spotlight.getLocationOnScreen(overlayLoc);
        r.offset(-overlayLoc[0], -overlayLoc[1]);
        spotlight.setHoleRect(r, padPx);
        return true;
    }

    @Nullable
    private static View findBottomNavItemView(BottomNavigationView nav, @IdRes int menuItemId) {
        try {
            Menu menu = nav.getMenu();
            ViewGroup menuView = (ViewGroup) nav.getChildAt(0);
            if (menuView == null || menu == null) return null;
            for (int i = 0; i < menu.size(); i++) {
                if (menu.getItem(i).getItemId() == menuItemId) {
                    if (i < menuView.getChildCount()) {
                        return menuView.getChildAt(i);
                    }
                    return null;
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "findBottomNavItemView", t);
        }
        return null;
    }

    /** Always persist tour seen — otherwise users who tap Skip/Back never set prefs and see tour on every cold start. */
    private void dismiss() {
        spotlightSeq++;
        try {
            SharedPreferences prefs = activity.getSharedPreferences(AppTourActivity.PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(AppTourActivity.KEY_TOUR_DONE, true).apply();
        } catch (Throwable ignored) {
        }
        removeOverlay();
    }

    private void removeOverlay() {
        if (overlay != null) {
            try {
                ViewGroup parent = (ViewGroup) overlay.getParent();
                if (parent != null) parent.removeView(overlay);
            } catch (Throwable ignore) {
            }
        }
        overlay = null;
        spotlight = null;
        titleView = null;
        messageView = null;
        stepProgressView = null;
        btnNext = null;
        btnSkip = null;
    }

    public void destroy() {
        spotlightSeq++;
        removeOverlay();
    }
}
