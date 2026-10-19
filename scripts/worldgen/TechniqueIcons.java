import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Draws the 32x32 icons of the Azure Cloud Sword Art's techniques, "jade night" style: a deep jade night, a thin bright
 * frame, each move traced in azure light (pixel art, no smoothing, so it stays crisp at 16 px in the combat bar).
 * Usage: java scripts/worldgen/TechniqueIcons.java src/main/resources/assets/murimblock/textures/gui/technique
 *   cloud_step_slash: a level draw-cut across, speed lines behind, the blade at its end (the dash);
 *   thousand_drizzle: six thin sharp cuts across the square at their own angles (the flurry's lines of light);
 *   heaven_splitting_crescent: a crescent rising, the blade pointed at the sky, speed lines under it, the ground below;
 *   swallow_returns: a swallow in profile turning back on its trail (the secret; red eye);
 *   flowing_cloud_stance: the passive, the blade held in guard over drifting cloud bands.
 * And the Long Fist's, "ember night": the same frame and light, warm (dark umber, amber light, a pale fist):
 *   tiger_step_punch: a fist thrown forward, streaks of the bound behind it, dust kicked up below;
 *   rapid_fists: a fist in the middle of a burst of small impacts and short streaks (the flurry);
 *   whirlwind_kick: a sweeping ring of motion round a foot at its end, dust at the ground;
 *   inch_force: a fist pressed to a line, a ring of white at the contact, waves going out beyond it (the secret; red);
 *   steady_breath: the passive, a closed fist under three rising lines of breath.
 * And the Sanguine Art's, "blood night": the same frame, crimson black, the moves in glowing blood crystal:
 *   bloodbane_drain (Blood Crystal): an open hand, blood rising from it into a great crystal, fragments circling it;
 *   scarlet_tear: the blood sword thrown point first, the great arc of its swing behind it, a burst where it strikes;
 *   devourer_of_worlds (Blood Crystal Form): a body in shadow, crystals bursting from its shoulders and back, its heart lit;
 *   crystal_barrage: a volley of crystals flying in a fan, speed lines behind them;
 *   crimson_stance: the passive, a fist in guard, drops of blood rising round it;
 *   blood_iceberg: the blood temple's arena, a wall of red bricks, gold spires lit at their tips, blood falling down it.
 * And the Celestial Peak Spear's, "peak night": a navy night, a gold frame, the spear in steel and azure, the Alliance's:
 *   piercing_dragon: the spear driven level across the square, a long trail behind it, the dragon's line;
 *   plum_blossom_thrusts: five spear points fanned out from one hand like a plum flower, a petal of light at each;
 *   thousand_soldier_sweep: the spear round in a full ring at the knees, dust thrown out from it;
 *   long_reach: the passive, a spear level with a gold mark at its point, 3 blocks out.
 * And the Traveller's Staff Art's, "road dusk": a deep moss dusk, a bronze frame, the staff in pale ash with iron shoes and
 * its red cord, the moves in warm light:
 *   vertical_strike: the staff brought straight down, its arc of light from over the head, a dull burst where it lands;
 *   point_flurry: the staff level, its point thrust out, four ghost points of the earlier thrusts behind it;
 *   spinning_rampart: the staff spinning before a dark figure, a full disc of light, an arrow stopped dead on it;
 *   third_resonance: the passive, three strikes in a row, the third a large ring of light (x2).
 * And the Mountain-Shattering Fist's, "stone night": a slate night, a bronze frame, broken rock, dust and embers:
 *   mountain_crushing_punch: a great fist driven right into a cone of rock bursting apart;
 *   earth_breaker: two fists brought down on cracked ground, a ring of shock, stones thrown up;
 *   bull_charge: a lowered shoulder bursting through a brick wall, bricks flying;
 *   sky_breaker: an uppercut rising from a column of earth, a body thrown up above it (the secret; red);
 *   heaven_falling_mountain: a fist falling like a meteor, wrapped in fire, onto a crater (the ultimate);
 *   earthshaker: the passive, a fist on the ground, cracks running out and the shock round it.
 * And the Seven Stars Sword's, "star night": a night-blue sky with faint stars, a silver frame, the florett in steel and
 * every move in white and pale blue starlight:
 *   shooting_star: a long streak of light across the square, a comet's head at its end, a cross where it cut;
 *   dipper_steps: three star sigils round a dark figure, joined by a line, the florett thrust at it from behind;
 *   orbiting_stars: a dark figure, seven stars in a ring round its chest, one flaring where it stops a blow;
 *   constellation_link: the florett raised, lines of light from its point to three stars, and between them;
 *   falling_star_rain: stars falling in long tails onto a circle on the ground;
 *   seven_stars_array: seven swords of light in a ring, their points on a figure held in the middle (the ultimate);
 *   polaris_riposte: a great pole star over a florett thrust (the secret; one red point);
 *   star_marks: the passive, the Big Dipper's seven stars joined, the brightest pulsing.
 * And the Purple Heaven Thunder Saber's, "storm night": a bruised violet night, a bronze frame cast with the thunder
 * pattern's corners, the zhanmadao in storm steel, every move in white and violet lightning with gold sparks:
 *   thunder_descends: the saber brought down from above onto cracked ground, a bolt striking the same spot;
 *   rolling_thunder: a ring of the saber's sweep round a dark figure, arcs leaping out of it;
 *   heavens_drum: the saber planted point down inside a cage of vertical arcs, an arrow stopped on it;
 *   severing_flash: a low level cut, a crescent of light flying off across the square;
 *   nine_heavens_wrath: a storm cloud over a ring on the ground, bolts falling into it;
 *   thunder_sovereign: a figure struck by a great bolt, lightning all round it (the ultimate);
 *   heaven_answers: a seam of light in the sky, a bolt down onto a parried blade (the secret; one red point);
 *   rolling_drums: the passive, three bronze drums, the last lit violet.
 * And the Golden Bell's, "temple dawn": a warm umber dusk, a gold frame, a monk's figure and his bell in gold:
 *   golden_bell: a monk in gold, palms joined, a bell of light over him;
 *   iron_rebound: an iron palm thrust out, a blow bouncing off it, sparks;
 *   rooted_mountain: a figure of stone in a deep stance, cracks in the ground round its feet;
 *   bell_toll: the bell ringing, its waves going out, a figure knocked flat (the secret; one red point);
 *   bell_palms: the passive (none): two open palms, plain.
 * And the Majin Fist's, "high noon": a deep crimson sky burning to gold at its heart, a gold frame, every move in the
 * white and gold of the sun (the Son of the Demon's art, after Escanor):
 *   sunfire_fist: a golden fist driven right, a burst of flame and light before it;
 *   pride_flare: an open palm thrust out, a wave of flame running low across the ground;
 *   cruel_sun: a small sun blazing over a raised palm, its rays round it;
 *   noon_judgment: three golden circles on the ground and three pillars of light falling into them;
 *   midday_meteor: a body falling fist first onto a crater, a ring of fire running out from it;
 *   the_one: a great sun filling the square, a small dark figure before it, arms flung wide (the ultimate);
 *   grand_noon: a column of white sunlight from the top of the square onto a fist (the secret; one red point);
 *   pride_of_the_sun: the passive, a fist wrapped in flame under a small sun.
 * And the Crescent Moon Halberd's, "lake night": a deep blue night over still water, a silver frame, the halberd in
 * night-blue lacquer and silver, every move in white and pale blue moonlight:
 *   twin_reflection: a dark foe between the halberd and a water-blue reflection striking it from behind;
 *   shattered_mirror: a reflection bursting into shards of water-glass flying out all round;
 *   swap: the halberd's bearer and a reflection, two curved arrows between them;
 *   hall_of_mirrors: four water-blue reflections round the bearer, each pointing at a foe;
 *   night_of_the_full_moon: a great full moon over the ground, beams of moonlight falling from it (the ultimate);
 *   mirror_image: the passive, a figure dodging aside, its reflection of water left where it stood.
 */
public class TechniqueIcons {
    static final int W = 32;
    static final String[] NAMES = {"cloud_step_slash", "thousand_drizzle", "heaven_splitting_crescent", "swallow_returns", "flowing_cloud_stance",
            "tiger_step_punch", "rapid_fists", "whirlwind_kick", "inch_force", "steady_breath",
            "bloodbane_drain", "scarlet_tear", "devourer_of_worlds", "crystal_barrage", "crimson_stance", "blood_iceberg",
            "piercing_dragon", "plum_blossom_thrusts", "thousand_soldier_sweep", "long_reach",
            "vertical_strike", "point_flurry", "spinning_rampart", "third_resonance",
            "mountain_crushing_punch", "earth_breaker", "bull_charge", "sky_breaker", "heaven_falling_mountain", "earthshaker",
            "shooting_star", "dipper_steps", "orbiting_stars", "constellation_link", "falling_star_rain", "seven_stars_array", "polaris_riposte", "star_marks",
            "thunder_descends", "rolling_thunder", "heavens_drum", "severing_flash", "nine_heavens_wrath", "thunder_sovereign", "heaven_answers", "rolling_drums",
            "golden_bell", "iron_rebound", "rooted_mountain", "bell_toll", "bell_palms",
            "sunfire_fist", "pride_flare", "cruel_sun", "noon_judgment", "midday_meteor", "the_one", "grand_noon", "pride_of_the_sun",
            "tyrants_thrust", "earth_splitting_sweep", "falling_mountain", "overlords_charge", "hundred_battles_rampage", "conqueror_descends",
            "breaking_the_cauldrons", "conquerors_weight",
            "lash", "centipede_bite", "devouring_hunger", "impaling_thorns", "burrow", "grapple", "constrict", "kagune_shift",
            "twin_reflection", "shattered_mirror", "swap", "hall_of_mirrors", "night_of_the_full_moon", "mirror_image"};
    /** Where the Long Fist's and the Sanguine Art's icons start in NAMES. */
    static final int FIST = 5, BLOOD = 10, SPEAR = 16, STAFF = 20, MOUNTAIN = 24, STARS = 30, THUNDER = 38, BELL = 46, SUN = 51, RUIN = 59, DEMON = 67, MOON = 75;

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        dir.mkdirs();
        for (int i = 0; i < NAMES.length; i++) {
            BufferedImage img = new BufferedImage(W, W, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            if (i < FIST) jade(g, img, i);
            else if (i < BLOOD) ember(g, img, i - FIST);
            else if (i < SPEAR) blood(g, img, i - BLOOD);
            else if (i < STAFF) peak(g, img, i - SPEAR);
            else if (i < MOUNTAIN) road(g, img, i - STAFF);
            else if (i < STARS) stone(g, img, i - MOUNTAIN);
            else if (i < THUNDER) stars(g, img, i - STARS);
            else if (i < BELL) thunder(g, img, i - THUNDER);
            else if (i < SUN) bell(g, img, i - BELL);
            else if (i < RUIN) sun(g, img, i - SUN);
            else if (i < DEMON) ruin(g, img, i - RUIN);
            else if (i < MOON) demon(g, img, i - DEMON);
            else moon(g, img, i - MOON);
            g.dispose();
            File f = new File(dir, NAMES[i] + ".png");
            ImageIO.write(img, "png", f);
            System.out.println("wrote " + f);
        }
    }


    // ------------------------------------------------------------ helpers

    static Color c(int r, int g, int b) { return new Color(r, g, b); }
    static Color c(int r, int g, int b, int a) { return new Color(r, g, b, a); }

    /** A stroke tapering from width w0 to w1 along a quadratic curve. */
    static void taper(Graphics2D g, double x0, double y0, double cx, double cy, double x1, double y1, double w0, double w1, Color col) {
        g.setColor(col);
        int n = 24;
        double[] lx = new double[n + 1], ly = new double[n + 1], rx = new double[n + 1], ry = new double[n + 1];
        for (int i = 0; i <= n; i++) {
            double t = (double) i / n, u = 1 - t;
            double px = u * u * x0 + 2 * u * t * cx + t * t * x1, py = u * u * y0 + 2 * u * t * cy + t * t * y1;
            double dx = 2 * u * (cx - x0) + 2 * t * (x1 - cx), dy = 2 * u * (cy - y0) + 2 * t * (y1 - cy);
            double len = Math.hypot(dx, dy);
            if (len < 1e-6) { dx = 1; dy = 0; len = 1; }
            double w = (w0 + (w1 - w0) * t) / 2;
            lx[i] = px - dy / len * w; ly[i] = py + dx / len * w;
            rx[i] = px + dy / len * w; ry[i] = py - dx / len * w;
        }
        Path2D p = new Path2D.Double();
        p.moveTo(lx[0], ly[0]);
        for (int i = 1; i <= n; i++) p.lineTo(lx[i], ly[i]);
        for (int i = n; i >= 0; i--) p.lineTo(rx[i], ry[i]);
        p.closePath();
        g.fill(p);
    }

    static void line(Graphics2D g, double x0, double y0, double x1, double y1, float w, Color col) {
        g.setColor(col);
        g.setStroke(new BasicStroke(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g.draw(new Line2D.Double(x0, y0, x1, y1));
    }

    static void px(BufferedImage img, int x, int y, Color col) {
        if (x >= 0 && y >= 0 && x < W && y < W) img.setRGB(x, y, col.getRGB());
    }

    static void disc(Graphics2D g, double cx, double cy, double r, Color col) {
        g.setColor(col);
        g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
    }

    /** A small sword: hilt at (x, y), blade toward angle (degrees, 0 = right), length len. */
    static void sword(Graphics2D g, double x, double y, double deg, double len, Color blade, Color edge, Color hilt) {
        double a = Math.toRadians(deg), dx = Math.cos(a), dy = Math.sin(a);
        taper(g, x, y, x + dx * len / 2, y + dy * len / 2, x + dx * len, y + dy * len, 2.6, 0.6, blade);
        line(g, x + dx * 1 - dy * 0.6, y + dy * 1 + dx * 0.6, x + dx * len * 0.9 - dy * 0.6, y + dy * len * 0.9 + dx * 0.6, 0.8f, edge);
        // Guard and grip.
        line(g, x - dy * 2.5, y + dx * 2.5, x + dy * 2.5, y - dx * 2.5, 1.6f, hilt);
        line(g, x, y, x - dx * 4, y - dy * 4, 1.8f, hilt.darker());
    }

    // ------------------------------------------------------------ A: jade night, glowing azure motion

    static void jadeBack(BufferedImage img, Graphics2D g) {
        for (int y = 0; y < W; y++) for (int x = 0; x < W; x++) {
            double d = Math.hypot(x - 15.5, y - 15.5) / 22.0;
            int r = (int) (10 + 12 * (1 - d)), gg = (int) (30 + 34 * (1 - d)), b = (int) (42 + 40 * (1 - d));
            img.setRGB(x, y, c(r, gg, b).getRGB());
        }
        // Thin bright frame.
        g.setColor(c(120, 190, 210));
        g.drawRect(0, 0, W - 1, W - 1);
        g.setColor(c(40, 80, 96));
        g.drawRect(1, 1, W - 3, W - 3);
    }

    static final Color AZ = c(140, 215, 255), AZ_DIM = c(70, 140, 190), WHITE = c(240, 250, 255), STEEL = c(200, 215, 225), GOLD = c(214, 172, 80);

    static void jade(Graphics2D g, BufferedImage img, int i) {
        jadeBack(img, g);
        switch (i) {
            case 0 -> { // Cloud Step Slash: a level draw-cut across, afterimages and speed lines behind.
                for (int k = 0; k < 4; k++) line(g, 3, 9 + k * 4, 10 + k % 2 * 3, 9 + k * 4, 1f, AZ_DIM);
                taper(g, 6, 20, 16, 12, 29, 15, 0.5, 4.2, c(90, 170, 220));
                taper(g, 7, 19, 17, 12.5, 29, 15, 0.3, 2.0, WHITE);
                sword(g, 24, 22, -20, 9, STEEL, WHITE, GOLD);
                for (int k = 0; k < 3; k++) px(img, 29 - k, 16 + k, WHITE);
            }
            case 1 -> { // Thousand Drizzle: six thin sharp cuts across the square at their own angles, Sukuna-style.
                for (double[] cut : CUTS) {
                    g.setColor(AZ_DIM);
                    lens(g, cut[0] - 1, cut[1] + 1, cut[2] - 1, cut[3] + 1, 2.6);
                }
                for (double[] cut : CUTS) {
                    g.setColor(WHITE);
                    lens(g, cut[0], cut[1], cut[2], cut[3], 1.6);
                }
            }
            case 2 -> { // Heaven-Splitting Crescent: a crescent rising, the blade at the sky, a crater below.
                g.setColor(c(90, 170, 220));
                g.fill(crescent(18, 16, 12, 4, 100, 260));
                g.setColor(WHITE);
                g.fill(crescent(18, 16, 12, 1.8, 105, 255));
                sword(g, 16, 13, -90, 12, STEEL, WHITE, GOLD);
                for (int k = 0; k < 3; k++) line(g, 11 + k * 5, 24, 11 + k * 5, 29, 1f, AZ_DIM);
                g.setColor(c(120, 95, 70));
                g.fillRect(6, 29, 20, 2);
                px(img, 9, 28, c(150, 120, 90)); px(img, 22, 28, c(150, 120, 90));
            }
            case 3 -> swallow(g, img, WHITE, AZ, c(200, 60, 50));
            default -> { // Flowing Cloud Stance: the blade held in guard over drifting cloud bands.
                for (int k = 0; k < 3; k++) {
                    g.setColor(k == 1 ? c(160, 210, 230) : c(110, 170, 200));
                    g.fill(new RoundRectangle2D.Double(3 + k * 4, 18 + k * 4, 22 - k * 3, 3, 3, 3));
                }
                sword(g, 9, 22, -45, 19, STEEL, WHITE, GOLD);
                disc(g, 24, 7, 1.2, WHITE);
            }
        }
    }

    // ------------------------------------------------------------ B: ember night, the Long Fist

    static void emberBack(BufferedImage img, Graphics2D g) {
        for (int y = 0; y < W; y++) for (int x = 0; x < W; x++) {
            double d = Math.hypot(x - 15.5, y - 15.5) / 22.0;
            int r = (int) (30 + 34 * (1 - d)), gg = (int) (16 + 18 * (1 - d)), b = (int) (12 + 10 * (1 - d));
            img.setRGB(x, y, c(r, gg, b).getRGB());
        }
        g.setColor(c(226, 168, 92));
        g.drawRect(0, 0, W - 1, W - 1);
        g.setColor(c(92, 52, 30));
        g.drawRect(1, 1, W - 3, W - 3);
    }

    static final Color AMBER = c(255, 190, 110), AMBER_DIM = c(168, 98, 56), WARM_WHITE = c(255, 246, 228),
            SKIN = c(236, 198, 160), SKIN_DARK = c(168, 112, 78), RED = c(214, 64, 48),
            SLEEVE = c(120, 38, 34), CUFF = c(230, 214, 180), PANTS = c(60, 52, 70);

    /**
     * A closed fist seen from the side, knuckles toward dir (1 right, -1 left), its middle at (x, y), s pixels tall:
     * the hand, the folds between the fingers, the thumb across below, a light on the knuckles.
     */
    static void fist(Graphics2D g, BufferedImage img, double x, double y, double s, int dir) {
        double w = s * 1.15, h = s;
        // The forearm in its sleeve, behind the fist, and the cuff.
        double back = x - dir * (w / 2 + s * 0.9);
        g.setColor(SLEEVE);
        g.fill(new Rectangle2D.Double(Math.min(back, x - dir * w * 0.3), y - h * 0.32, Math.abs(x - dir * w * 0.3 - back), h * 0.64));
        line(g, x - dir * w * 0.42, y - h * 0.34, x - dir * w * 0.42, y + h * 0.3, 1.4f, CUFF);
        g.setColor(SKIN_DARK);
        g.fill(new RoundRectangle2D.Double(x - w / 2 - 1, y - h / 2 - 1, w + 2, h + 2, s * 0.6, s * 0.6));
        g.setColor(SKIN);
        g.fill(new RoundRectangle2D.Double(x - w / 2, y - h / 2, w, h, s * 0.5, s * 0.5));
        double kx = x + dir * w * 0.22;
        for (int k = 1; k <= 3; k++) {
            int py = (int) Math.round(y - h / 2 + h * k / 4.0);
            line(g, kx - dir * w * 0.12, py, kx + dir * w * 0.32, py, 1f, SKIN_DARK);
        }
        line(g, x - dir * w * 0.35, y + h * 0.3, x + dir * w * 0.15, y + h * 0.3, 1.4f, SKIN_DARK);
        px(img, (int) Math.round(x + dir * w * 0.42), (int) Math.round(y - h * 0.32), WARM_WHITE);
    }

    /** A small impact: a four-pointed burst of radius r. */
    static void burst(Graphics2D g, double x, double y, double r, Color col) {
        g.setColor(col);
        Path2D p = new Path2D.Double();
        p.moveTo(x, y - r); p.lineTo(x + r * 0.28, y); p.lineTo(x, y + r); p.lineTo(x - r * 0.28, y); p.closePath();
        g.fill(p);
        Path2D q = new Path2D.Double();
        q.moveTo(x - r, y); q.lineTo(x, y - r * 0.28); q.lineTo(x + r, y); q.lineTo(x, y + r * 0.28); q.closePath();
        g.fill(q);
    }

    static void ember(Graphics2D g, BufferedImage img, int i) {
        emberBack(img, g);
        switch (i) {
            case 0 -> { // Tiger Step Punch: the bound's streaks behind, the fist thrown, dust below.
                for (int k = 0; k < 3; k++) taper(g, 3, 10 + k * 5, 10, 10 + k * 5, 18, 12 + k * 3, 0.3, 2.4 - k * 0.5, k == 1 ? AMBER : AMBER_DIM);
                fist(g, img, 23, 15, 9, 1);
                for (int k = 0; k < 3; k++) { px(img, 5 + k * 4, 27, AMBER_DIM); px(img, 6 + k * 4, 26, AMBER_DIM); }
                g.setColor(c(110, 80, 60));
                g.fillRect(3, 28, 26, 1);
            }
            case 1 -> { // Rapid Fists: a fist amid a burst of impacts and short streaks.
                double[][] hits = {{8, 8, 3}, {25, 8, 2.5}, {6, 23, 2.5}, {25, 24, 3}, {16, 5, 2}};
                for (double[] h : hits) burst(g, h[0], h[1], h[2], AMBER);
                for (int k = 0; k < 4; k++) line(g, 3, 13 + k * 2, 8 + k % 2 * 2, 13 + k * 2, 1f, AMBER_DIM);
                fist(g, img, 17, 17, 9, 1);
                burst(g, 27, 16, 2.5, WARM_WHITE);
            }
            case 2 -> { // Whirlwind Kick: a sweeping ring round a foot at its end, dust at the ground.
                g.setColor(AMBER_DIM);
                g.setStroke(new BasicStroke(3.2f));
                g.draw(new Arc2D.Double(4, 6, 22, 16, 30, 280, Arc2D.OPEN));
                g.setColor(AMBER);
                g.setStroke(new BasicStroke(1.4f));
                g.draw(new Arc2D.Double(4, 6, 22, 16, 60, 250, Arc2D.OPEN));
                // The leg along the end of the sweep, the boot at its end.
                taper(g, 12, 12, 15, 9, 21, 8.5, 4.2, 3.6, PANTS);
                g.setColor(c(80, 56, 40));
                g.fill(new RoundRectangle2D.Double(20, 6, 9, 5, 3, 3));
                g.setColor(CUFF);
                g.fillRect(19, 7, 1, 3);
                for (int k = 0; k < 5; k++) px(img, 6 + k * 5, 26 - k % 2, AMBER_DIM);
                g.setColor(c(110, 80, 60));
                g.fillRect(3, 28, 26, 1);
            }
            case 3 -> { // Inch Force: the fist pressed to a line, a white ring at the contact, red waves beyond.
                line(g, 18, 5, 18, 27, 2f, c(150, 110, 90));
                fist(g, img, 11, 16, 9, 1);
                g.setColor(WARM_WHITE);
                g.setStroke(new BasicStroke(1.2f));
                g.draw(new Ellipse2D.Double(15, 12, 7, 8));
                g.setColor(RED);
                for (int k = 0; k < 3; k++) g.draw(new Arc2D.Double(15 + k * 4, 9 + k, 9, 14 - k * 2, -60, 120, Arc2D.OPEN));
            }
            default -> { // Steady Breath: three rising lines of breath over a closed fist.
                for (int k = 0; k < 3; k++) {
                    g.setColor(k == 1 ? AMBER : AMBER_DIM);
                    g.setStroke(new BasicStroke(1.4f));
                    Path2D p = new Path2D.Double();
                    double x0 = 10 + k * 6;
                    p.moveTo(x0, 17);
                    p.curveTo(x0 - 3, 13, x0 + 3, 9, x0, 4);
                    g.draw(p);
                }
                fist(g, img, 16, 23, 10, 1);
            }
        }
    }

    // ------------------------------------------------------------ C: blood night, the Sanguine Art

    static void bloodBack(BufferedImage img, Graphics2D g) {
        for (int y = 0; y < W; y++) for (int x = 0; x < W; x++) {
            double d = Math.hypot(x - 15.5, y - 15.5) / 22.0;
            int r = (int) (26 + 34 * (1 - d)), gg = (int) (6 + 6 * (1 - d)), b = (int) (12 + 10 * (1 - d));
            img.setRGB(x, y, c(r, gg, b).getRGB());
        }
        g.setColor(c(232, 86, 98));
        g.drawRect(0, 0, W - 1, W - 1);
        g.setColor(c(96, 18, 30));
        g.drawRect(1, 1, W - 3, W - 3);
    }

    static final Color BLOOD_RED = c(204, 28, 48), BLOOD_DIM = c(120, 18, 34), CRYSTAL = c(238, 72, 96), CRYSTAL_DARK = c(150, 18, 40),
            CRYSTAL_LIGHT = c(255, 176, 190), PINK_WHITE = c(255, 232, 236), SHADOW = c(16, 4, 8);

    /**
     * A cut crystal from (x0, y0) to its point (x1, y1), w wide at its widest (a third of the way along): a dark side, a
     * lit side, a glint along the ridge.
     */
    static void crystal(Graphics2D g, double x0, double y0, double x1, double y1, double w) {
        double dx = x1 - x0, dy = y1 - y0, len = Math.hypot(dx, dy), nx = -dy / len * w / 2, ny = dx / len * w / 2;
        double mx = x0 + dx * 0.33, my = y0 + dy * 0.33;
        Path2D lit = new Path2D.Double(), dark = new Path2D.Double();
        lit.moveTo(x0, y0); lit.lineTo(mx + nx, my + ny); lit.lineTo(x1, y1); lit.closePath();
        dark.moveTo(x0, y0); dark.lineTo(mx - nx, my - ny); dark.lineTo(x1, y1); dark.closePath();
        g.setColor(CRYSTAL_DARK); g.fill(dark);
        g.setColor(CRYSTAL); g.fill(lit);
        line(g, mx + nx * 0.3, my + ny * 0.3, x1 - dx * 0.12, y1 - dy * 0.12, 0.8f, CRYSTAL_LIGHT);
    }

    /** A crystal of gold from (x0, y0) to its point (x1, y1), w wide: a dark side, a lit side, a glint. */
    static void crystalGold(Graphics2D g, double x0, double y0, double x1, double y1, double w, Color dark, Color light) {
        double dx = x1 - x0, dy = y1 - y0, len = Math.hypot(dx, dy), nx = -dy / len * w / 2, ny = dx / len * w / 2;
        double mx = x0 + dx * 0.33, my = y0 + dy * 0.33;
        Path2D lit = new Path2D.Double(), shade = new Path2D.Double();
        lit.moveTo(x0, y0); lit.lineTo(mx + nx, my + ny); lit.lineTo(x1, y1); lit.closePath();
        shade.moveTo(x0, y0); shade.lineTo(mx - nx, my - ny); shade.lineTo(x1, y1); shade.closePath();
        g.setColor(dark.darker()); g.fill(shade);
        g.setColor(dark); g.fill(lit);
        line(g, mx + nx * 0.3, my + ny * 0.3, x1 - dx * 0.12, y1 - dy * 0.12, 0.8f, light);
    }

    /** A drop of blood, its round foot at (x, y), r wide. */
    static void drop(Graphics2D g, double x, double y, double r, Color col) {
        g.setColor(col);
        Path2D p = new Path2D.Double();
        p.moveTo(x, y - r * 2.4);
        p.quadTo(x + r * 1.1, y - r * 0.6, x + r, y);
        p.quadTo(x, y + r * 1.3, x - r, y);
        p.quadTo(x - r * 1.1, y - r * 0.6, x, y - r * 2.4);
        g.fill(p);
    }

    // ------------------------------------------------------------ E: stone night, the Mountain-Shattering Fist

    static final Color STONE_BRONZE = c(196, 140, 72), SLATE = c(96, 98, 104), SLATE_LIGHT = c(150, 150, 156), SLATE_DARK = c(52, 54, 60),
            EARTH = c(124, 88, 58), EMBER = c(255, 150, 60), FIRE = c(255, 214, 110), WRAP = c(232, 222, 200), BRICK = c(150, 70, 52);

    static void stoneBack(BufferedImage img, Graphics2D g) {
        for (int y = 0; y < W; y++) for (int x = 0; x < W; x++) {
            double d = Math.hypot(x - 15.5, y - 15.5) / 22.0;
            img.setRGB(x, y, c((int) (22 + 24 * (1 - d)), (int) (22 + 22 * (1 - d)), (int) (26 + 22 * (1 - d))).getRGB());
        }
        g.setColor(STONE_BRONZE); g.drawRect(0, 0, W - 1, W - 1);
        g.setColor(c(70, 44, 26)); g.drawRect(1, 1, W - 3, W - 3);
    }

    /** A rock fragment: a small irregular quad round (x, y), size r, lit on top. */
    static void rock(Graphics2D g, BufferedImage img, double x, double y, double r) {
        Path2D p = new Path2D.Double();
        p.moveTo(x - r, y); p.lineTo(x - r * 0.3, y - r); p.lineTo(x + r, y - r * 0.4); p.lineTo(x + r * 0.5, y + r * 0.8); p.closePath();
        g.setColor(SLATE); g.fill(p);
        px(img, (int) Math.round(x - r * 0.3), (int) Math.round(y - r * 0.6), SLATE_LIGHT);
    }

    /** Cracks in the ground from (x, y): jagged dark lines out to the given ends. */
    static void cracks(Graphics2D g, double x, double y, double[][] ends) {
        for (double[] e : ends) {
            double mx = (x + e[0]) / 2 + (e[1] - y) * 0.15, my = (y + e[1]) / 2 - (e[0] - x) * 0.15;
            line(g, x, y, mx, my, 1.2f, c(16, 12, 10));
            line(g, mx, my, e[0], e[1], 0.9f, c(16, 12, 10));
            line(g, mx, my + 0.6, e[0], e[1] + 0.6, 0.5f, EMBER);
        }
    }

    /** A fist wrapped in white cloth (the warlord's), as fist() but bandaged across the knuckles. */
    static void wrappedFist(Graphics2D g, BufferedImage img, double x, double y, double s, int dir) {
        fist(g, img, x, y, s, dir);
        for (int k = 0; k < 3; k++) {
            double yy = y - s * 0.3 + k * s * 0.3;
            line(g, x - s * 0.5, yy, x + s * 0.5, yy + dir * 0.5, 1f, WRAP);
        }
    }

    static void stone(Graphics2D g, BufferedImage img, int i) {
        stoneBack(img, g);
        switch (i) {
            case 0 -> { // Mountain-Crushing Punch: the fist driven right into a cone of rock bursting apart.
                g.setColor(c(80, 70, 64));
                Path2D cone = new Path2D.Double();
                cone.moveTo(16, 16); cone.lineTo(30, 5); cone.lineTo(30, 27); cone.closePath();
                g.fill(cone);
                for (int k = 0; k < 4; k++) line(g, 17, 16, 30, 7 + k * 6, 0.8f, k % 2 == 0 ? EMBER : AMBER_DIM);
                rock(g, img, 26, 8, 2.2); rock(g, img, 28, 17, 2.6); rock(g, img, 24, 25, 2.0); rock(g, img, 21, 12, 1.4);
                wrappedFist(g, img, 10, 16, 8, 1);
                burst(g, 16, 16, 3, FIRE);
            }
            case 1 -> { // Earth Breaker: two fists down on cracked ground, a ring of shock, stones thrown up.
                g.setColor(EARTH); g.fillRect(2, 22, 28, 8);
                cracks(g, 16, 23, new double[][]{{3, 27}, {8, 30}, {24, 30}, {29, 26}, {16, 30}});
                g.setColor(AMBER_DIM); g.setStroke(new BasicStroke(1.2f)); g.draw(new Ellipse2D.Double(3, 18, 26, 9));
                g.setColor(FIRE); g.setStroke(new BasicStroke(0.8f)); g.draw(new Arc2D.Double(3, 18, 26, 9, 200, 140, Arc2D.OPEN));
                wrappedFist(g, img, 9, 14, 7, 1);
                wrappedFist(g, img, 23, 14, 7, -1);
                rock(g, img, 5, 9, 1.8); rock(g, img, 27, 8, 2.0); rock(g, img, 16, 4, 1.5);
            }
            case 2 -> { // Bull Charge: a lowered shoulder bursting through a brick wall, bricks flying.
                for (int row = 0; row < 6; row++) for (int col = 0; col < 2; col++) {
                    int bx = 22 + col * 5 - (row % 2) * 2, by = 4 + row * 4;
                    if (row >= 2 && row <= 3) continue;
                    g.setColor(BRICK); g.fillRect(bx, by, 4, 3);
                }
                for (double[] b : new double[][]{{28, 13}, {25, 18}, {30, 20}, {27, 10}}) { g.setColor(BRICK); g.fill(new Rectangle2D.Double(b[0] - 1.5, b[1] - 1, 3, 2)); }
                for (int k = 0; k < 3; k++) taper(g, 2, 10 + k * 5, 8, 11 + k * 5, 14, 13 + k * 3, 0.3, 2.2, k == 1 ? AMBER : AMBER_DIM);
                // The shoulder: a crimson pauldron, gold rim, the head down behind it.
                g.setColor(c(156, 31, 26)); g.fill(new Ellipse2D.Double(11, 9, 12, 11));
                g.setColor(c(214, 168, 60)); g.setStroke(new BasicStroke(1f)); g.draw(new Arc2D.Double(11, 9, 12, 11, -60, 120, Arc2D.OPEN));
                g.setColor(SKIN); g.fill(new Ellipse2D.Double(8, 6, 6, 6));
                burst(g, 22, 15, 3.5, FIRE);
            }
            case 3 -> { // Sky Breaker: an uppercut rising from a column of earth, a body thrown up above it.
                g.setColor(EARTH); g.fillRect(2, 26, 28, 4);
                g.setColor(c(110, 78, 52)); g.fill(new Rectangle2D.Double(11, 16, 9, 11));
                for (int k = 0; k < 3; k++) rock(g, img, 9 + k * 7, 15 - k % 2 * 3, 1.6);
                wrappedFist(g, img, 15, 12, 7, 1);
                line(g, 15, 18, 15, 22, 1f, AMBER_DIM);
                // The body flung high, small, and the red streaks of the secret.
                g.setColor(c(156, 31, 26)); g.fill(new Ellipse2D.Double(21, 2, 4, 4)); g.fill(new RoundRectangle2D.Double(20, 5, 6, 5, 2, 2));
                for (int k = 0; k < 3; k++) line(g, 19 + k * 3, 11, 21 + k * 3, 7, 1f, RED);
            }
            case 4 -> { // Heaven-Falling Mountain: a fist falling like a meteor, wrapped in fire, onto a crater.
                g.setColor(EARTH); g.fillRect(2, 25, 28, 5);
                g.setColor(c(36, 24, 18)); g.fill(new Arc2D.Double(7, 21, 18, 9, 180, 180, Arc2D.CHORD));
                for (int k = 0; k < 4; k++) taper(g, 26 - k * 2, 2, 22, 7, 17, 14, 0.3, 4.0 - k * 0.6, k % 2 == 0 ? EMBER : FIRE);
                disc(g, 15, 16, 6, c(255, 120, 40, 200));
                wrappedFist(g, img, 15, 16, 8, 1);
                for (double[] q : new double[][]{{5, 22}, {27, 21}, {9, 19}, {24, 18}}) rock(g, img, q[0], q[1], 1.4);
                burst(g, 16, 25, 3, FIRE);
            }
            default -> { // Earthshaker: a fist on the ground, cracks running out, the shock round it.
                g.setColor(EARTH); g.fillRect(2, 21, 28, 9);
                cracks(g, 16, 22, new double[][]{{2, 26}, {6, 30}, {26, 30}, {30, 24}, {13, 30}, {20, 30}});
                for (int k = 0; k < 3; k++) {
                    g.setColor(k == 0 ? FIRE : AMBER_DIM); g.setStroke(new BasicStroke(0.9f));
                    g.draw(new Arc2D.Double(16 - 5 - k * 4, 16 - 3 - k * 2, 10 + k * 8, 8 + k * 4, 0, 180, Arc2D.OPEN));
                }
                wrappedFist(g, img, 16, 15, 8, 1);
            }
        }
    }

    // ------------------------------------------------------------ D: peak night, the Celestial Peak Spear

    static final Color NAVY_GOLD = c(217, 178, 95), SPEAR_STEEL = c(214, 222, 230), STEEL_DARK = c(130, 140, 150), AZURE = c(43, 95, 168),
            AZURE_LIGHT = c(120, 170, 235), SHAFT = c(34, 52, 92);

    static void peakBack(BufferedImage img, Graphics2D g) {
        for (int y = 0; y < W; y++) for (int x = 0; x < W; x++) {
            double d = Math.hypot(x - 15.5, y - 15.5) / 22.0;
            img.setRGB(x, y, c((int) (10 + 14 * (1 - d)), (int) (16 + 22 * (1 - d)), (int) (34 + 40 * (1 - d))).getRGB());
        }
        g.setColor(NAVY_GOLD); g.drawRect(0, 0, W - 1, W - 1);
        g.setColor(c(60, 52, 30)); g.drawRect(1, 1, W - 3, W - 3);
    }

    /** A spear from its butt (x0, y0) to its point (x1, y1): a navy shaft, an azure socket, a steel leaf head. */
    static void spear(Graphics2D g, BufferedImage img, double x0, double y0, double x1, double y1) {
        double dx = x1 - x0, dy = y1 - y0, len = Math.hypot(dx, dy), ux = dx / len, uy = dy / len;
        double hx = x1 - ux * 6, hy = y1 - uy * 6;
        line(g, x0, y0, hx, hy, 1.6f, SHAFT);
        line(g, x0, y0, hx, hy, 0.6f, c(70, 96, 150));
        line(g, hx - ux * 1.5, hy - uy * 1.5, hx, hy, 2.2f, AZURE);
        Path2D head = new Path2D.Double();
        double nx = -uy * 1.6, ny = ux * 1.6;
        head.moveTo(hx, hy); head.lineTo(hx + ux * 2.5 + nx, hy + uy * 2.5 + ny); head.lineTo(x1, y1); head.lineTo(hx + ux * 2.5 - nx, hy + uy * 2.5 - ny); head.closePath();
        g.setColor(STEEL_DARK); g.fill(head);
        line(g, hx + ux * 0.5, hy + uy * 0.5, x1 - ux * 0.5, y1 - uy * 0.5, 0.7f, SPEAR_STEEL);
        px(img, (int) Math.round(hx - ux), (int) Math.round(hy - uy + 1), c(200, 60, 50));
    }

    static void peak(Graphics2D g, BufferedImage img, int i) {
        peakBack(img, g);
        switch (i) {
            case 0 -> { // Piercing Dragon: the spear driven level across, a long trail behind.
                for (int k = 0; k < 3; k++) line(g, 2, 14 + k * 2, 14, 15 + k * 1.2, 1f, k == 1 ? AZURE_LIGHT : AZURE);
                spear(g, img, 6, 17, 29, 15);
                burst(g, 29, 15, 2.5, AZURE_LIGHT);
            }
            case 1 -> { // Plum Blossom Thrusts: five points fanned from one hand, a petal of light at each.
                double[][] tips = {{27, 6}, {29, 13}, {28, 21}, {22, 27}, {20, 3}};
                for (double[] t : tips) spear(g, img, 5, 24, t[0], t[1]);
                for (double[] t : tips) { disc(g, t[0], t[1], 1.6, c(255, 200, 220)); px(img, (int) t[0], (int) t[1], c(255, 255, 255)); }
            }
            case 2 -> { // Sweep of a Thousand Soldiers: the spear round in a ring at the knees, dust thrown out.
                g.setColor(AZURE); g.setStroke(new BasicStroke(2f)); g.draw(new Ellipse2D.Double(3, 14, 26, 11));
                g.setColor(AZURE_LIGHT); g.setStroke(new BasicStroke(0.8f)); g.draw(new Arc2D.Double(3, 14, 26, 11, 200, 160, Arc2D.OPEN));
                spear(g, img, 16, 20, 29, 22);
                for (int[] q : new int[][]{{3, 27}, {6, 29}, {27, 28}, {29, 26}, {16, 29}}) px(img, q[0], q[1], c(190, 170, 130));
                // A small figure crouched in the middle.
                g.setColor(c(16, 22, 40)); g.fill(new Ellipse2D.Double(13, 10, 6, 6)); g.fill(new RoundRectangle2D.Double(12, 15, 8, 6, 3, 3));
            }
            default -> { // Long Reach: a spear level, a gold mark at its point 3 blocks out.
                spear(g, img, 3, 20, 27, 20);
                for (int k = 0; k < 3; k++) { g.setColor(NAVY_GOLD); g.fillRect(6 + k * 7, 25, 5, 1); }
                line(g, 27, 12, 27, 26, 1f, NAVY_GOLD);
                disc(g, 27, 20, 2.2, c(255, 230, 150));
            }
        }
    }

    // ------------------------------------------------------------ the Traveller's Staff: road dusk

    static final Color ASH = c(222, 196, 150), ASH_DARK = c(150, 116, 74), IRON = c(96, 102, 108), CORD = c(196, 48, 40),
            DUSK_LIGHT = c(255, 214, 140), DUSK_DIM = c(150, 110, 64), BRONZE = c(176, 132, 74), FIGURE = c(18, 24, 18);

    static void roadBack(BufferedImage img, Graphics2D g) {
        for (int y = 0; y < W; y++) for (int x = 0; x < W; x++) {
            double d = Math.hypot(x - 15.5, y - 15.5) / 22.0;
            img.setRGB(x, y, c((int) (22 + 22 * (1 - d)), (int) (28 + 26 * (1 - d)), (int) (16 + 12 * (1 - d))).getRGB());
        }
        g.setColor(BRONZE); g.drawRect(0, 0, W - 1, W - 1);
        g.setColor(c(70, 56, 30)); g.drawRect(1, 1, W - 3, W - 3);
    }

    /** A staff from (x0, y0) to (x1, y1): pale ash with a dark edge, iron shoes at both ends, a red cord a third along. */
    static void staff(Graphics2D g, BufferedImage img, double x0, double y0, double x1, double y1) {
        double dx = x1 - x0, dy = y1 - y0, len = Math.hypot(dx, dy), ux = dx / len, uy = dy / len;
        line(g, x0, y0, x1, y1, 2.0f, ASH_DARK);
        line(g, x0 - uy * 0.3, y0 + ux * 0.3, x1 - uy * 0.3, y1 + ux * 0.3, 1.0f, ASH);
        line(g, x0, y0, x0 + ux * 1.8, y0 + uy * 1.8, 2.2f, IRON);
        line(g, x1 - ux * 1.8, y1 - uy * 1.8, x1, y1, 2.2f, IRON);
        double cx = x0 + dx * 0.36, cy = y0 + dy * 0.36;
        line(g, cx - ux, cy - uy, cx + ux, cy + uy, 2.4f, CORD);
    }

    static void road(Graphics2D g, BufferedImage img, int i) {
        roadBack(img, g);
        switch (i) {
            case 0 -> { // Vertical Strike: the arc from over the head, the staff come down level ahead, the burst at its end.
                g.setColor(DUSK_DIM); g.setStroke(new BasicStroke(3f)); g.draw(new Arc2D.Double(2, 6, 26, 26, 95, -88, Arc2D.OPEN));
                g.setColor(DUSK_LIGHT); g.setStroke(new BasicStroke(1f)); g.draw(new Arc2D.Double(2, 6, 26, 26, 90, -78, Arc2D.OPEN));
                staff(g, img, 5, 23, 26, 19);
                burst(g, 27, 19, 3.2, DUSK_LIGHT);
                for (int[] q : new int[][]{{24, 28}, {28, 27}, {30, 29}}) px(img, q[0], q[1], c(170, 150, 110));
                line(g, 3, 29, 30, 29, 1f, c(60, 52, 30));
            }
            case 1 -> { // Flurry of Points: the staff thrust level, four ghost points behind its point.
                // The four thrusts before this one: short streaks of light ahead, each ending in its point.
                for (double y : new double[]{12, 16, 25.5, 29}) {
                    line(g, 16, y, 27, y, 1f, DUSK_DIM);
                    line(g, 22, y, 27, y, 1f, DUSK_LIGHT);
                    disc(g, 27.5, y, 1.0, c(255, 236, 190));
                }
                staff(g, img, 3, 21, 27, 21);
                burst(g, 28.5, 21, 2.6, DUSK_LIGHT);
            }
            case 2 -> { // Spinning Rampart: a full disc of light, the staff across it, an arrow stopped dead on it.
                g.setColor(c(255, 214, 140, 70)); g.fill(new Ellipse2D.Double(5, 4, 22, 22));
                g.setColor(DUSK_DIM); g.setStroke(new BasicStroke(1.6f)); g.draw(new Ellipse2D.Double(5, 4, 22, 22));
                g.setColor(DUSK_LIGHT); g.setStroke(new BasicStroke(0.8f)); g.draw(new Arc2D.Double(6, 5, 20, 20, 30, 120, Arc2D.OPEN));
                g.draw(new Arc2D.Double(6, 5, 20, 20, 210, 120, Arc2D.OPEN));
                staff(g, img, 8, 24, 24, 6);
                // The arrow, from the right, its head broken off on the disc.
                line(g, 26.5, 15, 29.5, 15, 1f, c(140, 110, 70));
                px(img, 29, 14, c(230, 230, 230)); px(img, 29, 16, c(230, 230, 230));
                burst(g, 26.5, 15, 2.2, c(255, 245, 220));
                // The figure behind, only its head and shoulders under the disc.
                g.setColor(FIGURE); g.fill(new Ellipse2D.Double(13, 25, 6, 5)); g.fill(new RoundRectangle2D.Double(9, 28, 14, 6, 4, 4));
            }
            default -> { // Third Resonance: two small strikes, then the third, a large ring of light.
                disc(g, 7, 22, 1.4, DUSK_DIM);
                disc(g, 13, 18, 1.8, DUSK_DIM);
                g.setColor(DUSK_LIGHT); g.setStroke(new BasicStroke(1.4f)); g.draw(new Ellipse2D.Double(16, 6, 13, 13));
                burst(g, 22.5, 12.5, 3.4, DUSK_LIGHT);
                staff(g, img, 3, 29, 21, 14);
                // "x2" under the ring, in bronze pixels.
                for (int[] q : new int[][]{{21, 22}, {23, 24}, {22, 23}, {21, 24}, {23, 22}, {25, 22}, {26, 22}, {26, 23}, {25, 24}, {25, 25}, {26, 25}})
                    px(img, q[0], q[1], BRONZE);
            }
        }
    }

    static void blood(Graphics2D g, BufferedImage img, int i) {
        bloodBack(img, g);
        switch (i) {
            case 0 -> { // Blood Crystal: an open hand, blood rising into a great crystal, fragments circling it.
                g.setColor(BLOOD_DIM);
                g.setStroke(new BasicStroke(1f));
                g.draw(new Ellipse2D.Double(5, 6, 22, 10));
                // The great crystal, its foot over the palm.
                crystal(g, 16, 19, 16, 1.5, 10);
                crystal(g, 6, 12, 3, 9, 2.6);
                crystal(g, 26, 10, 29, 7, 2.6);
                crystal(g, 21, 15, 24, 16.5, 2);
                // Blood rising from the palm.
                disc(g, 15.5, 20.5, 0.9, BLOOD_RED);
                disc(g, 16.5, 22.5, 0.9, CRYSTAL);
                // The open hand from below, palm up: the palm, four fingers and the thumb, outlined.
                g.setColor(SKIN_DARK);
                g.fill(new RoundRectangle2D.Double(9, 24, 14, 7, 4, 4));
                for (int k = 0; k < 4; k++) g.fill(new RoundRectangle2D.Double(9.5 + k * 3.2, 20.5, 3, 6, 2, 2));
                g.fill(new RoundRectangle2D.Double(22, 23, 5, 3, 2, 2));
                g.setColor(SKIN);
                g.fill(new RoundRectangle2D.Double(10, 25, 12, 5, 3, 3));
                for (int k = 0; k < 4; k++) g.fill(new RoundRectangle2D.Double(10 + k * 3.2, 21, 2, 5, 2, 2));
                g.fill(new RoundRectangle2D.Double(22.5, 23.5, 4, 2, 2, 2));
                px(img, 16, 27, CRYSTAL);
            }
            case 1 -> { // Scarlet Tear: the great arc of the swing behind, the blood sword thrown point first, a burst where it strikes.
                g.setColor(BLOOD_DIM);
                g.setStroke(new BasicStroke(3f));
                g.draw(new Arc2D.Double(3, 6, 24, 24, 100, 140, Arc2D.OPEN));
                g.setColor(BLOOD_RED);
                g.setStroke(new BasicStroke(1.2f));
                g.draw(new Arc2D.Double(3, 6, 24, 24, 110, 120, Arc2D.OPEN));
                // The sword: a long crystal blade, a dark red guard across it, a wrapped grip, flying up and right.
                crystal(g, 10, 23, 26, 7, 4.6);
                line(g, 7.5, 20.5, 12.5, 25.5, 2f, c(150, 30, 40));
                line(g, 10, 23, 5.5, 27.5, 2f, c(70, 14, 20));
                px(img, 7, 26, CRYSTAL); px(img, 5, 28, CRYSTAL_DARK);
                burst(g, 27, 6, 4.5, CRYSTAL_LIGHT);
                burst(g, 27, 6, 2.2, PINK_WHITE);
                for (int[] p : new int[][]{{29, 11}, {22, 3}, {30, 3}}) px(img, p[0], p[1], CRYSTAL);
            }
            case 2 -> { // Blood Crystal Form: a body in shadow, crystals bursting from it, its heart lit.
                for (double[] k : new double[][]{{10, 15, 3, 4}, {13, 12, 9, 2}, {16, 12, 16, 1}, {19, 12, 23, 2}, {22, 15, 29, 4}, {9, 19, 3, 15}, {23, 19, 29, 15}})
                    crystal(g, k[0], k[1], k[2], k[3], 3.2);
                g.setColor(SHADOW);
                g.fill(new Ellipse2D.Double(12, 9, 8, 8));
                g.fill(new RoundRectangle2D.Double(7, 17, 18, 14, 7, 7));
                g.setColor(c(80, 12, 24));
                g.setStroke(new BasicStroke(1f));
                g.draw(new RoundRectangle2D.Double(7, 17, 18, 14, 7, 7));
                disc(g, 16, 23, 2.6, BLOOD_RED);
                disc(g, 16, 23, 1.4, CRYSTAL_LIGHT);
                for (int[] p : new int[][]{{12, 21}, {20, 21}, {13, 27}, {19, 27}}) px(img, p[0], p[1], BLOOD_RED);
            }
            case 3 -> { // Crystal Barrage: a volley of crystals in a fan, speed lines behind.
                double[][] volley = {{4, 7, 15, 10}, {8, 14, 22, 14}, {5, 20, 17, 18}, {12, 25, 25, 21}, {14, 4, 27, 8}};
                for (double[] v : volley) line(g, v[0] - 3, v[1] + (v[1] - v[3]) * 0.25, v[0] + 1, v[1], 1f, BLOOD_DIM);
                for (double[] v : volley) crystal(g, v[0], v[1], v[2], v[3], 3);
                burst(g, 28, 14, 2.5, CRYSTAL_LIGHT);
            }
            case 5 -> { // Blood Iceberg: the blood temple's arena, its wall of red bricks, gold spires lit at their tips, blood falling.
                Color brick = c(118, 30, 24), brickDark = c(70, 14, 12), gold = c(208, 170, 58), goldLight = c(255, 236, 160), lantern = c(255, 200, 110);
                // The ground and the arena's floor seen over the near wall, a gold seal on it.
                g.setColor(SHADOW); g.fillRect(2, 27, 28, 3);
                g.setColor(c(92, 22, 18)); g.fill(new Ellipse2D.Double(5, 12, 22, 8));
                g.setColor(gold); g.setStroke(new BasicStroke(1f)); g.draw(new Ellipse2D.Double(10, 14, 12, 4));
                px(img, 16, 16, goldLight);
                // The near wall: courses of bricks, joints dark, crenels along its top.
                g.setColor(brick); g.fillRect(4, 18, 24, 10);
                g.setColor(brickDark);
                for (int y = 20; y < 28; y += 3) g.drawLine(4, y, 27, y);
                for (int y = 18, k = 0; y < 28; y += 3, k++) for (int x = 6 + (k % 2) * 2; x < 28; x += 5) g.drawLine(x, y, x, y + 2);
                g.setColor(brick); for (int x = 4; x < 28; x += 4) g.fillRect(x, 16, 2, 2);
                // Gold spires, each crowned with a lantern.
                for (int[] sp : new int[][]{{6, 6}, {16, 2}, {26, 6}}) {
                    crystalGold(g, sp[0], 18, sp[0], sp[1] + 3, 3.4, gold, goldLight);
                    g.setColor(lantern); g.fillRect(sp[0] - 1, sp[1], 3, 3);
                    px(img, sp[0], sp[1] + 1, c(255, 250, 230));
                }
                // Blood falling down the wall into the channel.
                for (int x : new int[]{11, 21}) { line(g, x, 18, x, 26, 1.6f, BLOOD_RED); px(img, x, 27, CRYSTAL); }
            }
            default -> { // Crimson Stance: a fist in guard, drops of blood rising round it.
                drop(g, 8, 10, 2, BLOOD_RED);
                drop(g, 24, 8, 1.6, CRYSTAL);
                drop(g, 16, 6, 1.3, BLOOD_DIM);
                fist(g, img, 16, 20, 10, 1);
                // A crystal over the knuckles.
                crystal(g, 20, 17, 26, 14, 2.4);
                for (int k = 0; k < 3; k++) px(img, 6 + k * 9, 28, BLOOD_DIM);
            }
        }
    }


    /** The flurry's six cuts as lines across the square (x0, y0, x1, y1): diagonals, a level one, an upright one. */
    static final double[][] CUTS = {
            {4, 6, 22, 18}, {8, 21, 29, 21}, {5, 26, 24, 12}, {17, 3, 15, 29}, {12, 28, 28, 6}, {3, 13, 27, 10}};

    /** A thin lens (sharp at both ends) from a to b, widest `w` in the middle. */
    static void lens(Graphics2D g, double ax, double ay, double bx, double by, double w) {
        double dx = bx - ax, dy = by - ay, len = Math.hypot(dx, dy), nx = -dy / len * w / 2, ny = dx / len * w / 2;
        Path2D p = new Path2D.Double();
        p.moveTo(ax, ay);
        p.quadTo((ax + bx) / 2 + nx * 2, (ay + by) / 2 + ny * 2, bx, by);
        p.quadTo((ax + bx) / 2 - nx * 2, (ay + by) / 2 - ny * 2, ax, ay);
        g.fill(p);
    }

    /** A crescent: the ring between two circles, between two angles (degrees, counter-clockwise from right, y down). */
    static Shape crescent(double cx, double cy, double r, double thick, double from, double to) {
        Area outer = new Area(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, from, to - from, Arc2D.PIE));
        double r2 = r - thick;
        outer.subtract(new Area(new Ellipse2D.Double(cx - r2 + thick * 0.6, cy - r2, 2 * r2, 2 * r2)));
        return outer;
    }

    static void swallow(Graphics2D g, BufferedImage img, Color body, Color trail, Color eye) {
        // Its trail curling back.
        g.setColor(trail);
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new Arc2D.Double(9, 8, 20, 20, 200, 250, Arc2D.OPEN));
        // A swallow in profile flying left and back: the body, the raised wing, the deep forked tail.
        taper(g, 7, 17, 13, 17, 21, 16, 5.2, 1.6, body);
        disc(g, 8, 16.5, 2.8, body);
        taper(g, 12, 15, 14, 5, 25, 3, 5.0, 0.5, body);
        taper(g, 14, 18, 17, 23, 22, 26, 3.0, 0.5, body);
        taper(g, 20, 16, 25, 14, 30, 10, 2.0, 0.5, body);
        taper(g, 20, 16, 25, 18, 30, 22, 2.0, 0.5, body);
        // The beak.
        Path2D p = new Path2D.Double();
        p.moveTo(5.5, 15.5); p.lineTo(2.5, 17); p.lineTo(5.5, 18); p.closePath();
        g.setColor(body);
        g.fill(p);
        px(img, 7, 16, eye);
    }


    // ------------------------------------------------------------ the Seven Stars Sword: star night

    static final Color STARLIGHT = c(236, 244, 255), STAR_BLUE = c(150, 196, 255), STAR_DIM = c(84, 116, 190), SILVER = c(196, 206, 224),
            NIGHT_FIGURE = c(10, 14, 32);
    static final double[][] DIPPER = {{-2.2, 0.1}, {-1.2, 0.35}, {-0.4, 0.4}, {0.4, 0.4}, {1.9, 1.0}, {2.0, -0.1}, {0.8, -0.6}};
    static final int[][] DIPPER_LINES = {{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 3}};

    static void starBack(BufferedImage img, Graphics2D g) {
        java.util.Random r = new java.util.Random(7);
        for (int y = 0; y < W; y++) for (int x = 0; x < W; x++) {
            double d = Math.hypot(x - 15.5, y - 15.5) / 22.0;
            img.setRGB(x, y, c((int) (8 + 12 * (1 - d)), (int) (12 + 18 * (1 - d)), (int) (30 + 42 * (1 - d))).getRGB());
        }
        for (int k = 0; k < 14; k++) px(img, 2 + r.nextInt(28), 2 + r.nextInt(28), c(70, 90, 140));
        g.setColor(SILVER); g.drawRect(0, 0, W - 1, W - 1);
        g.setColor(c(46, 56, 96)); g.drawRect(1, 1, W - 3, W - 3);
    }

    /** A four-pointed star at (x, y), arms of length r, a white heart. */
    static void star(BufferedImage img, Graphics2D g, double x, double y, double r, Color col) {
        line(g, x - r, y, x + r, y, 1f, col);
        line(g, x, y - r, x, y + r, 1f, col);
        if (r >= 3) { line(g, x - r * 0.45, y - r * 0.45, x + r * 0.45, y + r * 0.45, 1f, STAR_DIM); line(g, x - r * 0.45, y + r * 0.45, x + r * 0.45, y - r * 0.45, 1f, STAR_DIM); }
        px(img, (int) Math.round(x), (int) Math.round(y), STARLIGHT);
    }

    /** A florett from its pommel (x0, y0) to its point (x1, y1): a thin steel blade, a star guard, a blue grip. */
    static void florett(Graphics2D g, BufferedImage img, double x0, double y0, double x1, double y1) {
        double dx = x1 - x0, dy = y1 - y0, len = Math.hypot(dx, dy), ux = dx / len, uy = dy / len;
        double gx = x0 + ux * 5, gy = y0 + uy * 5;
        line(g, x0, y0, gx, gy, 1.6f, c(34, 48, 96));
        line(g, gx, gy, x1, y1, 1.2f, SILVER);
        line(g, gx + ux, gy + uy, x1 - ux, y1 - uy, 0.5f, STARLIGHT);
        line(g, gx - uy * 3, gy + ux * 3, gx + uy * 3, gy - ux * 3, 1.4f, SILVER);
        px(img, (int) Math.round(gx), (int) Math.round(gy), c(70, 110, 220));
        px(img, (int) Math.round(x0), (int) Math.round(y0), STARLIGHT);
    }

    static void figure(Graphics2D g, double x, double y) {
        g.setColor(NIGHT_FIGURE);
        g.fill(new Ellipse2D.Double(x - 2.5, y - 9, 5, 5));
        g.fill(new RoundRectangle2D.Double(x - 3.5, y - 4, 7, 10, 3, 3));
    }

    static void stars(Graphics2D g, BufferedImage img, int i) {
        starBack(img, g);
        switch (i) {
            case 0 -> { // Shooting Star: a long streak across, the comet's head at its end, a cross where it cut.
                taper(g, 2, 24, 14, 18, 27, 10, 0.4, 2.6, STAR_DIM);
                taper(g, 6, 22, 16, 16, 27, 10, 0.2, 1.2, STAR_BLUE);
                line(g, 14, 19, 27, 10, 0.6f, STARLIGHT);
                star(img, g, 27, 10, 4, STARLIGHT);
                star(img, g, 15, 18, 2, STAR_BLUE);
            }
            case 1 -> { // Dipper Steps: three sigils round a figure, joined, the florett thrust at it from behind.
                double[][] at = {{6, 26}, {16, 28}, {26, 25}};
                for (int k = 0; k < 3; k++) {
                    g.setColor(STAR_DIM); g.draw(new Ellipse2D.Double(at[k][0] - 3, at[k][1] - 1.5, 6, 3));
                    px(img, (int) at[k][0], (int) at[k][1], STARLIGHT);
                    if (k > 0) line(g, at[k - 1][0], at[k - 1][1], at[k][0], at[k][1], 0.6f, STAR_BLUE);
                }
                figure(g, 16, 18);
                florett(g, img, 29, 10, 19, 15);
                star(img, g, 18, 15, 3, STARLIGHT);
            }
            case 2 -> { // Orbiting Stars: seven stars in a ring round a figure, one flaring.
                figure(g, 16, 20);
                g.setColor(STAR_DIM); g.draw(new Ellipse2D.Double(4, 13, 24, 9));
                for (int k = 0; k < 7; k++) {
                    double a = k * Math.PI * 2 / 7 + 0.4;
                    star(img, g, 16 + Math.cos(a) * 12, 17.5 + Math.sin(a) * 4.5, k == 0 ? 3.5 : 1.5, k == 0 ? STARLIGHT : STAR_BLUE);
                }
                line(g, 28, 18, 31, 15, 1f, c(200, 80, 70));
            }
            case 3 -> { // Constellation Link: the florett raised, lines to three stars and between them.
                florett(g, img, 6, 29, 9, 12);
                double[][] st = {{18, 6}, {26, 12}, {22, 22}};
                for (double[] q : st) line(g, 9, 12, q[0], q[1], 0.7f, STAR_BLUE);
                line(g, 18, 6, 26, 12, 0.6f, STARLIGHT); line(g, 26, 12, 22, 22, 0.6f, STARLIGHT);
                for (double[] q : st) star(img, g, q[0], q[1], 3, STARLIGHT);
                star(img, g, 9, 12, 2, STARLIGHT);
            }
            case 4 -> { // Falling Star Rain: stars falling in tails onto a circle.
                g.setColor(STAR_BLUE); g.draw(new Ellipse2D.Double(4, 23, 24, 6));
                g.setColor(STAR_DIM); g.draw(new Ellipse2D.Double(7, 24.5, 18, 3));
                double[][] f = {{10, 22}, {18, 15}, {24, 24}, {14, 8}};
                for (double[] q : f) { taper(g, q[0] - 7, q[1] - 9, q[0] - 3, q[1] - 4, q[0], q[1], 0.3, 1.6, STAR_BLUE); star(img, g, q[0], q[1], 2, STARLIGHT); }
            }
            case 5 -> { // Seven Stars Sword Array: seven swords of light in a ring, points on a figure.
                figure(g, 16, 20);
                for (int k = 0; k < 7; k++) {
                    double a = k * Math.PI * 2 / 7 - Math.PI / 2;
                    double sx = 16 + Math.cos(a) * 13, sy = 16 + Math.sin(a) * 13, ex = 16 + Math.cos(a) * 6, ey = 16 + Math.sin(a) * 6;
                    line(g, sx, sy, ex, ey, 1.6f, STAR_DIM);
                    line(g, sx, sy, ex, ey, 0.6f, STARLIGHT);
                }
                star(img, g, 16, 14, 3, STARLIGHT);
            }
            case 6 -> { // Polaris Riposte: a great pole star over a florett thrust.
                florett(g, img, 3, 27, 22, 16);
                star(img, g, 22, 9, 6, STARLIGHT);
                px(img, 30, 30, c(220, 60, 60));
            }
            default -> { // Star Marks: the Big Dipper, joined, the brightest pulsing.
                double sc = 5.2, cx = 16, cy = 17;
                for (int[] l : DIPPER_LINES) line(g, cx + DIPPER[l[0]][0] * sc, cy - DIPPER[l[0]][1] * sc, cx + DIPPER[l[1]][0] * sc, cy - DIPPER[l[1]][1] * sc, 0.6f, STAR_BLUE);
                for (int k = 0; k < 7; k++) star(img, g, cx + DIPPER[k][0] * sc, cy - DIPPER[k][1] * sc, k == 4 ? 3.5 : 2, k == 4 ? STARLIGHT : STAR_BLUE);
            }
        }
    }

    // ------------------------------------------------------------ storm night (the Purple Heaven Thunder Saber)

    static final Color BOLT = c(250, 246, 255), BOLT_VIOLET = c(181, 140, 255), BOLT_DIM = c(96, 64, 150), STORM_BRONZE = c(196, 154, 62),
            STORM_BRONZE_DARK = c(122, 90, 34), STORM_STEEL = c(120, 128, 146), STORM_FIGURE = c(14, 10, 24);

    static void stormBack(BufferedImage img, Graphics2D g) {
        java.util.Random r = new java.util.Random(11);
        for (int y = 0; y < W; y++) for (int x = 0; x < W; x++) {
            double d = Math.hypot(x - 15.5, y - 12) / 24.0;
            img.setRGB(x, y, c((int) (16 + 22 * (1 - d)), (int) (10 + 12 * (1 - d)), (int) (28 + 40 * (1 - d))).getRGB());
        }
        for (int k = 0; k < 6; k++) px(img, 2 + r.nextInt(28), 2 + r.nextInt(14), c(70, 54, 110));
        g.setColor(STORM_BRONZE); g.drawRect(0, 0, W - 1, W - 1);
        g.setColor(STORM_BRONZE_DARK); g.drawRect(1, 1, W - 3, W - 3);
        // The thunder pattern's square spirals in the corners.
        for (int[] q : new int[][] {{2, 2}, {W - 6, 2}, {2, W - 6}, {W - 6, W - 6}}) {
            g.setColor(STORM_BRONZE);
            g.drawLine(q[0], q[1], q[0] + 3, q[1]); g.drawLine(q[0] + 3, q[1], q[0] + 3, q[1] + 3); g.drawLine(q[0] + 3, q[1] + 3, q[0] + 1, q[1] + 3); g.drawLine(q[0] + 1, q[1] + 3, q[0] + 1, q[1] + 2);
        }
    }

    /** A jagged bolt from (x0, y0) to (x1, y1): a white core over a violet glow. */
    static void bolt(Graphics2D g, double x0, double y0, double x1, double y1, int seed, float w) {
        java.util.Random r = new java.util.Random(seed);
        int n = 5;
        double[] xs = new double[n + 1], ys = new double[n + 1];
        for (int i = 0; i <= n; i++) {
            double t = (double) i / n, amp = (i == 0 || i == n) ? 0 : 2.4;
            xs[i] = x0 + (x1 - x0) * t + (r.nextDouble() - 0.5) * 2 * amp;
            ys[i] = y0 + (y1 - y0) * t + (r.nextDouble() - 0.5) * amp;
        }
        for (int i = 0; i < n; i++) line(g, xs[i], ys[i], xs[i + 1], ys[i + 1], w + 1.4f, BOLT_DIM);
        for (int i = 0; i < n; i++) line(g, xs[i], ys[i], xs[i + 1], ys[i + 1], w + 0.6f, BOLT_VIOLET);
        for (int i = 0; i < n; i++) line(g, xs[i], ys[i], xs[i + 1], ys[i + 1], w * 0.6f, BOLT);
    }

    /** The zhanmadao from its pommel (x0, y0) to its point (x1, y1): a broad storm-steel blade, a bronze guard, a violet grip. */
    static void zhanmadao(Graphics2D g, BufferedImage img, double x0, double y0, double x1, double y1) {
        double dx = x1 - x0, dy = y1 - y0, len = Math.hypot(dx, dy), ux = dx / len, uy = dy / len;
        double gx = x0 + ux * 7, gy = y0 + uy * 7;
        line(g, x0, y0, gx, gy, 1.8f, c(94, 58, 146));
        taper(g, gx, gy, (gx + x1) / 2, (gy + y1) / 2, x1, y1, 3.0, 3.8, STORM_STEEL);
        line(g, gx + ux * 2 - uy * 1.4, gy + uy * 2 + ux * 1.4, x1 - uy * 1.6, y1 + ux * 1.6, 0.8f, c(232, 238, 245));
        line(g, gx + ux * 2, gy + uy * 2, gx + ux * 8, gy + uy * 8, 0.6f, c(242, 201, 76));
        line(g, gx - uy * 3, gy + ux * 3, gx + uy * 3, gy - ux * 3, 1.8f, STORM_BRONZE);
        px(img, (int) Math.round(x0), (int) Math.round(y0), STORM_BRONZE);
    }

    static void stormFigure(Graphics2D g, double x, double y) {
        g.setColor(STORM_FIGURE);
        g.fill(new Ellipse2D.Double(x - 2.5, y - 9, 5, 5));
        g.fill(new RoundRectangle2D.Double(x - 3.5, y - 4, 7, 10, 3, 3));
    }

    static void thunder(Graphics2D g, BufferedImage img, int i) {
        stormBack(img, g);
        switch (i) {
            case 0 -> { // Thunder Descends: the saber down onto cracked ground, a bolt on the same spot.
                bolt(g, 20, 2, 18, 25, 3, 1.2f);
                zhanmadao(g, img, 6, 6, 17, 25);
                g.setColor(BOLT_VIOLET);
                for (int k = 0; k < 6; k++) { double a = Math.PI + k * Math.PI / 5; line(g, 18, 27, 18 + Math.cos(a) * 9, 27 + Math.sin(a) * 2.5, 0.6f, BOLT_VIOLET); }
                line(g, 4, 28, 28, 28, 1f, c(60, 44, 80));
            }
            case 1 -> { // Rolling Thunder: the sweep's ring round a figure, arcs leaping out.
                stormFigure(g, 16, 20);
                g.setColor(BOLT_VIOLET); g.setStroke(new BasicStroke(1.6f)); g.draw(new Ellipse2D.Double(3, 14, 26, 9));
                g.setColor(BOLT); g.setStroke(new BasicStroke(0.6f)); g.draw(new Arc2D.Double(3, 14, 26, 9, 200, 140, Arc2D.OPEN));
                bolt(g, 27, 17, 30, 6, 5, 0.6f);
                bolt(g, 5, 18, 2, 8, 9, 0.6f);
            }
            case 2 -> { // Heaven's Drum: the saber planted in a cage of arcs, an arrow stopped on it.
                for (int k = 0; k < 4; k++) bolt(g, 6 + k * 6.5, 29, 7 + k * 6, 5, 20 + k, 0.4f);
                zhanmadao(g, img, 16, 4, 16, 28);
                line(g, 30, 14, 24, 15, 1f, c(150, 120, 80));
                px(img, 23, 15, BOLT);
            }
            case 3 -> { // Severing Flash: a low cut, a crescent flying off.
                zhanmadao(g, img, 3, 26, 14, 21);
                g.setColor(BOLT_VIOLET); g.setStroke(new BasicStroke(2.2f)); g.draw(new Arc2D.Double(12, 5, 18, 22, -60, 120, Arc2D.OPEN));
                g.setColor(BOLT); g.setStroke(new BasicStroke(0.8f)); g.draw(new Arc2D.Double(12, 5, 18, 22, -50, 100, Arc2D.OPEN));
                line(g, 6, 16, 18, 16, 0.6f, BOLT_DIM);
            }
            case 4 -> { // Nine Heavens' Wrath: a storm cloud, bolts into a ring on the ground.
                g.setColor(c(50, 40, 70)); g.fill(new Ellipse2D.Double(3, 3, 26, 8)); g.setColor(c(70, 56, 96)); g.fill(new Ellipse2D.Double(7, 2, 16, 6));
                g.setColor(BOLT_VIOLET); g.setStroke(new BasicStroke(1f)); g.draw(new Ellipse2D.Double(5, 24, 22, 6));
                bolt(g, 10, 9, 9, 26, 31, 0.6f); bolt(g, 17, 10, 16, 27, 32, 1.0f); bolt(g, 23, 9, 23, 26, 33, 0.6f);
            }
            case 5 -> { // Thunder Sovereign's Descent: a figure struck by a great bolt, lightning all round it.
                bolt(g, 16, 0, 16, 14, 41, 1.8f);
                stormFigure(g, 16, 22);
                bolt(g, 9, 14, 6, 26, 42, 0.4f); bolt(g, 23, 14, 26, 26, 43, 0.4f);
                g.setColor(BOLT_VIOLET); g.draw(new Ellipse2D.Double(7, 26, 18, 4));
            }
            case 6 -> { // Heaven Answers: a seam in the sky, a bolt down onto a parried blade.
                line(g, 6, 5, 26, 5, 1.6f, BOLT_VIOLET); line(g, 8, 5, 24, 5, 0.6f, BOLT);
                bolt(g, 16, 6, 18, 20, 51, 0.9f);
                zhanmadao(g, img, 4, 28, 24, 18);
                px(img, 30, 30, c(220, 60, 60));
            }
            default -> { // Rolling Drums: three bronze drums, the last lit violet.
                for (int k = 0; k < 3; k++) {
                    int x = 3 + k * 9, y = 12;
                    g.setColor(STORM_BRONZE_DARK); g.fillRect(x, y, 8, 11);
                    g.setColor(k == 2 ? BOLT_VIOLET : c(52, 40, 74)); g.fillRect(x + 1, y + 2, 6, 8);
                    g.setColor(k == 2 ? c(242, 201, 76) : STORM_BRONZE); g.fillRect(x, y, 8, 2);
                }
                bolt(g, 25, 2, 23, 11, 61, 0.5f);
            }
        }
    }

    // ------------------------------------------------------------ the Golden Bell: temple dawn

    static final Color GOLD_LIGHT = c(255, 214, 110), GOLD_DEEP = c(200, 140, 40), MONK = c(46, 30, 22), IRON_GREY = c(150, 158, 170), STONE = c(132, 124, 112);

    static void templeBack(BufferedImage img, Graphics2D g) {
        for (int y = 0; y < W; y++) for (int x = 0; x < W; x++) {
            double d = Math.hypot(x - 15.5, y - 15.5) / 22.0;
            img.setRGB(x, y, c((int) (34 + 30 * (1 - d)), (int) (22 + 18 * (1 - d)), (int) (14 + 8 * (1 - d))).getRGB());
        }
        g.setColor(GOLD_DEEP); g.drawRect(0, 0, W - 1, W - 1);
        g.setColor(c(80, 56, 24)); g.drawRect(1, 1, W - 3, W - 3);
    }

    /** A monk standing: round head, robe widening to the feet. */
    static void monk(Graphics2D g, double x, double y, Color col) {
        g.setColor(col);
        g.fill(new Ellipse2D.Double(x - 2.5, y - 11, 5, 5));
        Path2D robe = new Path2D.Double();
        robe.moveTo(x - 3, y - 6); robe.lineTo(x + 3, y - 6); robe.lineTo(x + 5, y + 6); robe.lineTo(x - 5, y + 6); robe.closePath();
        g.fill(robe);
    }

    /** A bell outline over (x, y): dome, waist, flared lip. */
    static void bellShape(Graphics2D g, double x, double y, double s, Color col, float w) {
        Path2D b = new Path2D.Double();
        b.moveTo(x - 2 * s, y - 9 * s);
        b.curveTo(x - 2 * s, y - 11.5 * s, x + 2 * s, y - 11.5 * s, x + 2 * s, y - 9 * s);
        b.curveTo(x + 2.6 * s, y - 4 * s, x + 3 * s, y - 1 * s, x + 5 * s, y);
        b.lineTo(x - 5 * s, y);
        b.curveTo(x - 3 * s, y - 1 * s, x - 2.6 * s, y - 4 * s, x - 2 * s, y - 9 * s);
        g.setColor(col); g.setStroke(new BasicStroke(w)); g.draw(b);
    }

    static void bell(Graphics2D g, BufferedImage img, int i) {
        templeBack(img, g);
        switch (i) {
            case 0 -> { // Golden Bell: a monk in gold, palms joined, a bell of light over him.
                monk(g, 16, 22, GOLD_DEEP);
                disc(g, 16, 12, 2.4, GOLD_LIGHT);
                line(g, 14, 18, 18, 18, 1.2f, GOLD_LIGHT);
                bellShape(g, 16, 28, 2.3, GOLD_LIGHT, 1.2f);
                px(img, 16, 3, c(255, 250, 220));
            }
            case 1 -> { // Iron Rebound: an iron palm thrust out, a blow bouncing off it, sparks.
                g.setColor(IRON_GREY); g.fill(new RoundRectangle2D.Double(5, 12, 10, 8, 3, 3));
                for (int k = 0; k < 4; k++) line(g, 15, 13 + k * 2, 18, 13 + k * 2, 1.4f, IRON_GREY);
                line(g, 28, 8, 19, 15, 1.4f, c(200, 90, 70));
                line(g, 19, 17, 27, 26, 1.4f, GOLD_LIGHT);
                for (int[] q : new int[][]{{21, 12}, {23, 16}, {21, 20}, {24, 19}}) px(img, q[0], q[1], c(255, 240, 200));
            }
            case 2 -> { // Rooted Mountain: a figure of stone in a deep stance, cracks round its feet.
                g.setColor(STONE);
                g.fill(new Ellipse2D.Double(13.5, 6, 5, 5));
                g.fill(new RoundRectangle2D.Double(11, 11, 10, 8, 3, 3));
                line(g, 12, 19, 7, 26, 2.4f, STONE); line(g, 20, 19, 25, 26, 2.4f, STONE);
                line(g, 3, 27, 29, 27, 1f, c(90, 70, 50));
                for (double[] q : new double[][]{{7, 27, 3, 30}, {25, 27, 29, 30}, {16, 27, 15, 30}}) line(g, q[0], q[1], q[2], q[3], 1f, c(60, 44, 30));
            }
            case 3 -> { // Bell Toll: the bell ringing, its waves, a figure knocked flat.
                bellShape(g, 11, 22, 1.6, GOLD_LIGHT, 1.2f);
                for (int k = 0; k < 3; k++) { g.setColor(GOLD_DEEP); g.setStroke(new BasicStroke(1f)); g.draw(new Arc2D.Double(8 - k * 3, 8 - k * 3, 12 + k * 6, 16 + k * 6, -40, 80, Arc2D.OPEN)); }
                g.setColor(MONK); g.fill(new RoundRectangle2D.Double(19, 25, 10, 3, 2, 2)); g.fill(new Ellipse2D.Double(26, 23, 4, 4));
                px(img, 30, 30, c(220, 60, 60));
            }
            default -> { // No passive: two open palms.
                for (int sx : new int[] {10, 22}) {
                    g.setColor(c(214, 170, 120)); g.fill(new RoundRectangle2D.Double(sx - 4, 12, 8, 10, 3, 3));
                    for (int k = 0; k < 4; k++) line(g, sx - 3 + k * 2, 12, sx - 3 + k * 2, 8, 1.4f, c(214, 170, 120));
                }
            }
        }
    }

    // ------------------------------------------------------------ the Majin Fist: "high noon"

    static final Color SUN_WHITE = c(255, 250, 225), SUN_GOLD = c(255, 200, 70), SUN_DEEP = c(230, 120, 30), SUN_GLOW = c(255, 210, 90, 120);

    static void sunBack(BufferedImage img, Graphics2D g) {
        for (int y = 0; y < W; y++) for (int x = 0; x < W; x++) {
            double d = Math.min(1, Math.hypot(x - 15.5, y - 13) / 21.0);
            int r = (int) (70 + 120 * (1 - d)), gr = (int) (14 + 70 * (1 - d) * (1 - d)), b = (int) (12 + 10 * (1 - d));
            img.setRGB(x, y, c(r, gr, b).getRGB());
        }
        g.setColor(c(230, 180, 60)); g.drawRect(0, 0, W - 1, W - 1);
        g.setColor(c(100, 40, 16)); g.drawRect(1, 1, W - 3, W - 3);
    }

    /** A sun: a soft glow, a gold disc, a white heart, rays. */
    static void sunDisc(Graphics2D g, double x, double y, double r, int rays) {
        disc(g, x, y, r * 1.8, c(255, 190, 70, 70));
        for (int k = 0; k < rays; k++) {
            double a = Math.PI * 2 * k / rays;
            line(g, x + Math.cos(a) * r * 1.1, y + Math.sin(a) * r * 1.1, x + Math.cos(a) * r * 1.7, y + Math.sin(a) * r * 1.7, 1f, SUN_GOLD);
        }
        disc(g, x, y, r, SUN_GOLD);
        disc(g, x - r * 0.15, y - r * 0.15, r * 0.6, SUN_WHITE);
    }

    /** A golden fist (the colossus's skin). */
    static void goldFist(Graphics2D g, BufferedImage img, double x, double y, double s, int dir) {
        g.setColor(c(206, 150, 50)); g.fill(new RoundRectangle2D.Double(x - s / 2, y - s / 2, s, s * 0.9, 3, 3));
        g.setColor(c(250, 210, 110)); g.fill(new RoundRectangle2D.Double(x - s / 2 + 1, y - s / 2 + 1, s - 2, 2, 2, 2));
        for (int k = 1; k < 4; k++) line(g, x - s / 2 + k * s / 4, y - s / 2 + 1, x - s / 2 + k * s / 4, y, 0.6f, c(140, 90, 26));
        g.setColor(c(206, 150, 50)); g.fill(new Rectangle2D.Double(dir > 0 ? x - s / 2 - s * 0.6 : x + s / 2, y - s * 0.25, s * 0.6, s * 0.5));
    }

    static void sun(Graphics2D g, BufferedImage img, int i) {
        sunBack(img, g);
        switch (i) {
            case 0 -> { // Sunfire Fist: a golden fist driven right, a burst of flame and light before it.
                for (int k = 0; k < 3; k++) taper(g, 3, 13 + k * 3, 8, 14 + k * 2, 12, 16, 0.3, 1.6, k == 1 ? SUN_GOLD : SUN_DEEP);
                disc(g, 23, 16, 8, c(255, 170, 50, 110));
                burst(g, 23, 16, 6, SUN_GOLD);
                disc(g, 23, 16, 3, SUN_WHITE);
                goldFist(g, img, 15, 16, 8, 1);
            }
            case 1 -> { // Pride Flare: an open palm thrust out, a wave of flame running low across the ground.
                g.setColor(c(60, 20, 10)); g.fillRect(2, 25, 28, 5);
                for (int k = 0; k < 6; k++) {
                    double x = 10 + k * 3.6, h = 4 + Math.sin(k * 1.3) * 2 + k * 0.4;
                    taper(g, x, 26, x + 1, 26 - h * 0.6, x + 2, 26 - h, 2.6, 0.3, k % 2 == 0 ? SUN_DEEP : SUN_GOLD);
                }
                line(g, 9, 25, 30, 25, 1f, SUN_WHITE);
                g.setColor(c(206, 150, 50)); g.fill(new RoundRectangle2D.Double(3, 10, 6, 10, 2, 2));
                for (int k = 0; k < 4; k++) g.fill(new RoundRectangle2D.Double(8, 10 + k * 2.5, 4, 2, 1, 1));
                disc(g, 10, 15, 3, SUN_GLOW);
            }
            case 2 -> { // Cruel Sun: a small sun blazing over a raised palm.
                sunDisc(g, 16, 10, 5.5, 12);
                g.setColor(c(206, 150, 50)); g.fill(new RoundRectangle2D.Double(13, 19, 6, 11, 2, 2));
                for (int k = 0; k < 4; k++) g.fill(new RoundRectangle2D.Double(12 + k * 2, 16, 1.6, 4, 1, 1));
            }
            case 3 -> { // Noon Judgment: three golden circles, three pillars of light falling into them.
                double[][] at = {{7, 25}, {16, 27}, {25, 24}};
                for (double[] q : at) {
                    g.setColor(SUN_GOLD); g.setStroke(new BasicStroke(1f)); g.draw(new Ellipse2D.Double(q[0] - 4, q[1] - 1.5, 8, 3));
                    g.setColor(c(255, 230, 150, 150)); g.fill(new Rectangle2D.Double(q[0] - 1.5, 2, 3, q[1] - 2));
                    line(g, q[0], 2, q[0], q[1], 0.8f, SUN_WHITE);
                    disc(g, q[0], q[1], 2, SUN_WHITE);
                }
            }
            case 4 -> { // Midday Meteor: falling fist first onto a crater, a ring of fire.
                g.setColor(c(60, 20, 10)); g.fillRect(2, 25, 28, 5);
                g.setColor(c(30, 10, 6)); g.fill(new Arc2D.Double(9, 22, 14, 7, 180, 180, Arc2D.CHORD));
                g.setColor(SUN_DEEP); g.setStroke(new BasicStroke(1.2f)); g.draw(new Ellipse2D.Double(3, 23, 26, 6));
                for (int k = 0; k < 3; k++) taper(g, 8 + k * 2, 2, 12, 8, 15, 14, 0.3, 3.5 - k, k % 2 == 0 ? SUN_GOLD : SUN_DEEP);
                disc(g, 16, 16, 5, SUN_GLOW);
                goldFist(g, img, 16, 17, 7, -1);
            }
            case 5 -> { // The One: a great sun filling the square, a dark figure before it, arms flung wide.
                sunDisc(g, 16, 13, 10, 16);
                g.setColor(c(30, 12, 6));
                g.fill(new Ellipse2D.Double(14, 17, 4, 4));
                g.fill(new RoundRectangle2D.Double(13.5, 20.5, 5, 7, 2, 2));
                line(g, 14, 22, 8, 18, 1.4f, c(30, 12, 6));
                line(g, 18, 22, 24, 18, 1.4f, c(30, 12, 6));
                line(g, 15, 27, 14, 30, 1.4f, c(30, 12, 6));
                line(g, 17, 27, 18, 30, 1.4f, c(30, 12, 6));
            }
            case 6 -> { // Grand Noon: a column of white sunlight onto a fist (the secret).
                g.setColor(c(255, 240, 190, 170)); g.fill(new Rectangle2D.Double(11, 2, 10, 20));
                g.setColor(SUN_WHITE); g.fill(new Rectangle2D.Double(14, 2, 4, 20));
                sunDisc(g, 16, 4, 3, 8);
                goldFist(g, img, 16, 23, 7, 1);
                px(img, 30, 30, c(220, 60, 60));
            }
            default -> { // Pride of the Sun: a fist wrapped in flame under a small sun.
                sunDisc(g, 23, 8, 3.5, 10);
                for (int k = 0; k < 5; k++) taper(g, 10 + k * 2.5, 26, 9 + k * 2.5, 18, 12 + k * 2, 11 + (k % 2) * 2, 2.4, 0.3, k % 2 == 0 ? SUN_DEEP : SUN_GOLD);
                goldFist(g, img, 14, 21, 9, 1);
            }
        }
    }
    // ------------------------------------------------------------ the Overlord Spear: black iron, blood red, embers

    static final Color RUIN_IRON = c(40, 40, 46), RUIN_EDGE = c(240, 236, 222), RUIN_EMBER = c(255, 106, 36), RUIN_HOT = c(255, 214, 122),
            RUIN_BLOOD = c(176, 24, 24), RUIN_GOLD = c(217, 168, 63);

    static void ruinBack(BufferedImage img, Graphics2D g) {
        for (int y = 0; y < W; y++) for (int x = 0; x < W; x++) {
            double d = Math.min(1, Math.hypot(x - 15.5, y - 16) / 21.0);
            img.setRGB(x, y, c((int) (18 + 70 * (1 - d) * (1 - d)), (int) (10 + 10 * (1 - d)), (int) (12 + 8 * (1 - d))).getRGB());
        }
        g.setColor(RUIN_GOLD); g.drawRect(0, 0, W - 1, W - 1);
        g.setColor(c(70, 16, 14)); g.drawRect(1, 1, W - 3, W - 3);
    }

    /** The Spear of Ruin from its butt to its point: a black shaft with embers, a red mane, gold claws, a black head split by a burning seam. */
    static void ruinSpear(Graphics2D g, BufferedImage img, double x0, double y0, double x1, double y1) {
        double dx = x1 - x0, dy = y1 - y0, len = Math.hypot(dx, dy), ux = dx / len, uy = dy / len, nx = -uy, ny = ux;
        double hx = x1 - ux * 8, hy = y1 - uy * 8;
        line(g, x0, y0, hx, hy, 1.8f, RUIN_IRON);
        for (double t : new double[]{0.3, 0.6}) px(img, (int) Math.round(x0 + (hx - x0) * t), (int) Math.round(y0 + (hy - y0) * t), RUIN_EMBER);
        // The mane, the claws.
        line(g, hx - ux * 1.5, hy - uy * 1.5, hx - ux * 1.5 + nx * 3 - ux * 2, hy - uy * 1.5 + ny * 3 - uy * 2, 1.6f, RUIN_BLOOD);
        line(g, hx - ux * 0.5 + nx, hy - uy * 0.5 + ny, hx + ux * 1.5 + nx * 1.6, hy + uy * 1.5 + ny * 1.6, 1f, RUIN_GOLD);
        line(g, hx - ux * 0.5 - nx, hy - uy * 0.5 - ny, hx + ux * 1.5 - nx * 1.6, hy + uy * 1.5 - ny * 1.6, 1f, RUIN_GOLD);
        // The head: a broad leaf of black steel, bright edges, the burning seam.
        Path2D head = new Path2D.Double();
        head.moveTo(hx, hy); head.lineTo(hx + ux * 3 + nx * 2.4, hy + uy * 3 + ny * 2.4); head.lineTo(x1, y1);
        head.lineTo(hx + ux * 3 - nx * 2.4, hy + uy * 3 - ny * 2.4); head.closePath();
        g.setColor(RUIN_EDGE); g.fill(head);
        Path2D inner = new Path2D.Double();
        inner.moveTo(hx + ux * 0.6, hy + uy * 0.6); inner.lineTo(hx + ux * 3 + nx * 1.5, hy + uy * 3 + ny * 1.5); inner.lineTo(x1 - ux * 1.2, y1 - uy * 1.2);
        inner.lineTo(hx + ux * 3 - nx * 1.5, hy + uy * 3 - ny * 1.5); inner.closePath();
        g.setColor(RUIN_IRON); g.fill(inner);
        line(g, hx + ux * 0.8, hy + uy * 0.8, x1 - ux * 1.6, y1 - uy * 1.6, 0.8f, RUIN_EMBER);
    }

    static void ruin(Graphics2D g, BufferedImage img, int i) {
        ruinBack(img, g);
        switch (i) {
            case 0 -> { // Tyrant's Thrust: the spear driven right, a beam of red light out of its point.
                g.setColor(c(200, 30, 20, 120)); g.fill(new Rectangle2D.Double(17, 12, 14, 8));
                g.setColor(c(255, 120, 60, 200)); g.fill(new Rectangle2D.Double(17, 14, 14, 4));
                line(g, 17, 16, 31, 16, 1f, RUIN_HOT);
                ruinSpear(g, img, 2, 20, 19, 16);
            }
            case 1 -> { // Earth-Splitting Sweep: the spear planted, a ring of cracks round it, stones thrown up.
                g.setColor(c(50, 16, 10)); g.fill(new Rectangle2D.Double(2, 22, 28, 8));
                g.setColor(RUIN_EMBER); g.setStroke(new BasicStroke(1.2f)); g.draw(new Ellipse2D.Double(3, 21, 26, 8));
                cracks(g, 16, 25, new double[][]{{4, 24}, {28, 26}, {9, 29}, {24, 29}});
                rock(g, img, 6, 14, 2); rock(g, img, 26, 12, 2); rock(g, img, 21, 7, 1.5);
                ruinSpear(g, img, 17, 3, 15, 26);
            }
            case 2 -> { // Falling Mountain: the spear point down into a crater, cracks and dust.
                g.setColor(c(50, 16, 10)); g.fill(new Rectangle2D.Double(2, 24, 28, 6));
                g.setColor(c(20, 8, 6)); g.fill(new Arc2D.Double(7, 21, 18, 8, 180, 180, Arc2D.CHORD));
                cracks(g, 16, 26, new double[][]{{3, 25}, {29, 25}, {8, 30}, {25, 30}});
                for (int k = 0; k < 3; k++) line(g, 10 + k * 6, 2, 12 + k * 4, 9, 0.8f, c(255, 140, 60, 150));
                ruinSpear(g, img, 20, 2, 16, 25);
            }
            case 3 -> { // Overlord's Charge: the spear level, three figures on the point, a wall cracking ahead.
                g.setColor(c(80, 70, 66)); g.fill(new Rectangle2D.Double(27, 6, 4, 22));
                cracks(g, 27, 16, new double[][]{{30, 8}, {30, 26}, {31, 16}});
                for (int k = 0; k < 3; k++) { g.setColor(c(20, 12, 12)); g.fill(new Ellipse2D.Double(18 + k * 2.6, 11, 3, 3)); g.fill(new Rectangle2D.Double(18 + k * 2.6, 14, 3, 6)); }
                for (int k = 0; k < 3; k++) line(g, 1, 14 + k * 3, 7, 14 + k * 3, 0.8f, c(255, 150, 80, 140));
                ruinSpear(g, img, 3, 18, 27, 17);
            }
            case 4 -> { // Hundred Battles Rampage: a fan of thrusts, each ending in a small burst.
                double[][] at = {{28, 8}, {30, 14}, {29, 20}, {26, 25}, {24, 5}};
                for (double[] q : at) { line(g, 9, 17, q[0] - 3, q[1], 0.7f, c(240, 236, 222, 140)); disc(g, q[0], q[1], 2.2, c(255, 120, 40, 200)); disc(g, q[0], q[1], 1, RUIN_HOT); }
                ruinSpear(g, img, 2, 21, 20, 15);
            }
            case 5 -> { // Conqueror Descends: a giant spear falling point first onto a red circle, a small figure under it.
                g.setColor(RUIN_BLOOD); g.setStroke(new BasicStroke(1.4f)); g.draw(new Ellipse2D.Double(3, 23, 26, 7));
                g.setColor(c(255, 90, 30, 90)); g.fill(new Ellipse2D.Double(6, 24, 20, 5));
                ruinSpear(g, img, 16, -6, 16, 24);
                g.setColor(c(20, 10, 10)); g.fill(new Ellipse2D.Double(25, 21, 2.4, 2.4)); g.fill(new Rectangle2D.Double(25, 23.4, 2.4, 4));
            }
            case 6 -> { // Breaking the Cauldrons: a cracked bronze cauldron, an enormous beam over it (the secret).
                g.setColor(c(200, 30, 20, 140)); g.fill(new Rectangle2D.Double(2, 4, 28, 10));
                g.setColor(c(255, 170, 90, 220)); g.fill(new Rectangle2D.Double(2, 7, 28, 4));
                line(g, 2, 9, 30, 9, 1f, RUIN_HOT);
                g.setColor(c(122, 90, 46)); g.fill(new Arc2D.Double(8, 15, 16, 14, 180, 180, Arc2D.CHORD));
                g.fill(new Rectangle2D.Double(9, 19, 14, 2)); line(g, 11, 27, 10, 30, 1.4f, c(122, 90, 46)); line(g, 21, 27, 22, 30, 1.4f, c(122, 90, 46));
                cracks(g, 16, 22, new double[][]{{12, 27}, {20, 28}, {16, 29}});
                px(img, 30, 30, c(220, 60, 60));
            }
            default -> { // Conqueror's Weight: five embers over the spear's head, the fifth blazing.
                for (int k = 0; k < 5; k++) disc(g, 6 + k * 5, 6, k == 4 ? 2.4 : 1.6, k == 4 ? RUIN_HOT : RUIN_EMBER);
                ruinSpear(g, img, 4, 28, 26, 12);
            }
        }
    }

    // ------------------------------------------------------------ the Devouring Demon Art: dried blood, red flesh, purple centipedes

    static final Color FLESH = c(214, 36, 48), FLESH_LIGHT = c(255, 112, 112), FLESH_DARK = c(120, 14, 24),
            CHITIN = c(132, 52, 196), CHITIN_LIGHT = c(190, 120, 240), CHITIN_DARK = c(58, 18, 92);

    static void demonBack(BufferedImage img, Graphics2D g) {
        for (int y = 0; y < W; y++) for (int x = 0; x < W; x++) {
            double d = Math.hypot(x - 15.5, y - 17) / 22.0;
            int r = (int) (22 + 34 * (1 - d)), gg = (int) (6 + 6 * (1 - d)), b = (int) (10 + 10 * (1 - d));
            img.setRGB(x, y, c(r, gg, b).getRGB());
        }
        g.setColor(c(170, 40, 52));
        g.drawRect(0, 0, W - 1, W - 1);
        g.setColor(c(60, 10, 16));
        g.drawRect(1, 1, W - 3, W - 3);
    }

    /** A centipede along a quadratic curve: plates from tail to head, a leg each side, the head with its fangs. */
    static void centipede(Graphics2D g, double x0, double y0, double cx, double cy, double x1, double y1, int plates) {
        for (int i = 0; i <= plates; i++) {
            double t = (double) i / plates, u = 1 - t;
            double px = u * u * x0 + 2 * u * t * cx + t * t * x1, py = u * u * y0 + 2 * u * t * cy + t * t * y1;
            double dx = 2 * u * (cx - x0) + 2 * t * (x1 - cx), dy = 2 * u * (cy - y0) + 2 * t * (y1 - cy), len = Math.hypot(dx, dy);
            double nx = -dy / len, ny = dx / len, r = 1.7 + 1.3 * t;
            line(g, px + nx * r, py + ny * r, px + nx * (r + 1.6), py + ny * (r + 1.6) + 0.6, 0.8f, CHITIN_DARK);
            line(g, px - nx * r, py - ny * r, px - nx * (r + 1.6), py - ny * (r + 1.6) + 0.6, 0.8f, CHITIN_DARK);
            disc(g, px, py, r, i % 2 == 0 ? CHITIN : CHITIN_DARK);
            disc(g, px - nx * r * 0.35, py - ny * r * 0.35, r * 0.4, i % 2 == 0 ? CHITIN_LIGHT : FLESH);
        }
        // The fangs, red, open.
        double dx = x1 - cx, dy = y1 - cy, len = Math.hypot(dx, dy), fx = dx / len, fy = dy / len;
        line(g, x1, y1, x1 + fx * 3 - fy * 2.2, y1 + fy * 3 + fx * 2.2, 1.2f, FLESH_LIGHT);
        line(g, x1, y1, x1 + fx * 3 + fy * 2.2, y1 + fy * 3 - fx * 2.2, 1.2f, FLESH_LIGHT);
    }

    static void demon(Graphics2D g, BufferedImage img, int i) {
        demonBack(img, g);
        switch (i) {
            case 0 -> { // Lash: four tentacles whipping through an arc from the lower left, motion lines on the arc.
                g.setColor(c(255, 150, 140, 110));
                g.setStroke(new BasicStroke(1f));
                g.draw(new Arc2D.Double(3, 3, 26, 26, 200, -140, Arc2D.OPEN));
                g.draw(new Arc2D.Double(7, 7, 18, 18, 200, -130, Arc2D.OPEN));
                double[][] tips = {{4, 9}, {12, 4}, {22, 5}, {28, 12}};
                for (int k = 0; k < tips.length; k++) {
                    double[] t = tips[k];
                    taper(g, 7, 28, 4 + k * 5, 18 - k, t[0], t[1], 5.2, 1.6, FLESH_DARK);
                    taper(g, 7, 28, 4 + k * 5, 18 - k, t[0], t[1], 3.4, 0.8, k % 2 == 0 ? FLESH : FLESH_LIGHT);
                }
                disc(g, 7, 28, 2.6, FLESH_DARK);
            }
            case 1 -> // Centipede Bite: a centipede arching over from the lower left, coming down fangs first on the right.
                    centipede(g, 4, 28, 9, -1, 26, 21, 15);
            case 2 -> { // Devouring Hunger: the ring of Fury, a demon's red eye in it.
                for (int y = 0; y < W; y++) for (int x = 0; x < W; x++) {
                    double r = Math.hypot(x - 15.5, y - 15.5), a = Math.atan2(Math.abs(x - 15.5), y - 15.5) / Math.PI;
                    if (r >= 10 && r < 13.5 && a <= 0.82) px(img, x, y, r >= 12.5 ? FLESH_DARK : a > 0.5 ? FLESH : c(176, 18, 28));
                    else if (r >= 10 && r < 13.5) px(img, x, y, c(40, 8, 12));
                }
                g.setColor(c(14, 4, 6));
                g.fill(new Ellipse2D.Double(8.5, 12.5, 14, 7));
                disc(g, 15.5, 16, 3, FLESH);
                disc(g, 15.5, 16, 1.4, c(255, 220, 200));
                line(g, 9, 11, 22, 13, 1.2f, CHITIN);
            }
            case 3 -> { // Impaling Thorns: the ground broken, red thorns bursting up out of it, leaning out.
                g.setColor(c(46, 20, 16));
                g.fillRect(2, 24, 28, 6);
                g.setColor(c(92, 52, 36));
                g.fillRect(2, 24, 28, 1);
                double[][] thorns = {{9, 26, 5, 6}, {15, 26, 15, 3}, {21, 26, 25, 7}, {12, 26, 10, 12}, {19, 26, 21, 13}};
                for (double[] t : thorns) {
                    taper(g, t[0], t[1], (t[0] + t[2]) / 2, (t[1] + t[3]) / 2 + 2, t[2], t[3], 4.2, 0.6, FLESH_DARK);
                    taper(g, t[0], t[1], (t[0] + t[2]) / 2, (t[1] + t[3]) / 2 + 2, t[2], t[3], 2.6, 0.4, FLESH);
                }
                // Clods thrown up.
                for (int[] d : new int[][]{{5, 20}, {26, 18}, {8, 15}, {24, 12}}) px(img, d[0], d[1], c(110, 64, 44));
            }
            case 4 -> { // Burrow: a centipede bursting up out of a hole in the ground and arching over.
                g.setColor(c(46, 20, 16));
                g.fillRect(2, 25, 28, 5);
                g.setColor(c(14, 6, 6));
                g.fill(new Ellipse2D.Double(5, 23, 9, 4));
                centipede(g, 10, 26, 6, 0, 22, 12, 13);
                for (int[] d : new int[][]{{4, 20}, {15, 21}, {6, 16}}) px(img, d[0], d[1], c(110, 64, 44));
            }
            case 5 -> { // Grapple: a long tentacle shot out from the lower left, its hooked tip sunk in a foe's dark shape.
                g.setColor(c(96, 74, 70));
                g.fill(new Ellipse2D.Double(19, 4, 10, 14));
                taper(g, 4, 28, 12, 14, 23, 10, 3.6, 1.2, FLESH_DARK);
                taper(g, 4, 28, 12, 14, 23, 10, 2.2, 0.8, FLESH);
                // The hook, curled back.
                line(g, 23, 10, 26, 8, 1.4f, FLESH_LIGHT);
                line(g, 26, 8, 25, 12, 1.2f, FLESH_LIGHT);
                // Pulled taut: two lines of strain.
                line(g, 9, 19, 12, 22, 0.8f, c(255, 150, 140));
                line(g, 13, 15, 16, 18, 0.8f, c(255, 150, 140));
            }
            case 6 -> { // Constrict: a dark foe's shape, three turns of a centipede tight round it.
                g.setColor(c(96, 74, 70));
                g.fill(new Ellipse2D.Double(11, 3, 10, 8));
                g.fillRect(11, 10, 10, 18);
                for (int ring = 0; ring < 3; ring++) {
                    double y = 12 + ring * 6, tilt = ring % 2 == 0 ? 2 : -2;
                    g.setColor(CHITIN_DARK);
                    g.setStroke(new BasicStroke(3.2f));
                    g.draw(new Line2D.Double(7, y + tilt, 25, y - tilt));
                    g.setColor(ring == 1 ? CHITIN_LIGHT : CHITIN);
                    g.setStroke(new BasicStroke(1.6f));
                    g.draw(new Line2D.Double(7, y + tilt, 25, y - tilt));
                    for (int k = 0; k < 5; k++) px(img, 8 + k * 4, (int) Math.round(y + tilt - (k * 4 + 1) * tilt / 9) + 2, CHITIN_DARK);
                }
                // The head, its red fangs.
                line(g, 25, 10, 28, 8, 1.2f, FLESH_LIGHT);
                line(g, 25, 10, 28, 12, 1.2f, FLESH_LIGHT);
            }
            case 7 -> { // Kagune Shift: red tentacles on the left, a purple centipede on the right, a turning arrow between.
                taper(g, 6, 28, 3, 17, 7, 6, 3.6, 0.8, FLESH_DARK);
                taper(g, 6, 28, 3, 17, 7, 6, 2.2, 0.5, FLESH);
                taper(g, 9, 28, 10, 18, 13, 10, 3.0, 0.7, FLESH_DARK);
                taper(g, 9, 28, 10, 18, 13, 10, 1.8, 0.4, FLESH_LIGHT);
                centipede(g, 24, 29, 30, 16, 23, 6, 8);
                g.setColor(c(240, 220, 210));
                g.setStroke(new BasicStroke(1.2f));
                g.draw(new Arc2D.Double(11, 9, 10, 10, 30, 240, Arc2D.OPEN));
                line(g, 19.5, 17, 21.5, 14.5, 1.2f, c(240, 220, 210));
                line(g, 19.5, 17, 17, 16, 1.2f, c(240, 220, 210));
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------ the Crescent Moon Halberd: lake night, moonlight, silver

    static final Color MOON_WHITE = c(244, 251, 255), MOON_BLUE = c(159, 216, 255), MOON_DEEP = c(60, 120, 210), MOON_SILVER = c(200, 207, 221),
            LACQUER = c(34, 48, 92);

    static void moonBack(BufferedImage img, Graphics2D g) {
        for (int y = 0; y < W; y++) for (int x = 0; x < W; x++) {
            double d = Math.min(1, Math.hypot(x - 15.5, y - 13) / 22.0);
            int water = y > 24 ? 10 : 0;
            img.setRGB(x, y, c((int) (8 + 22 * (1 - d) * (1 - d)), (int) (12 + 30 * (1 - d) * (1 - d)) + water, (int) (30 + 60 * (1 - d) * (1 - d)) + water * 2).getRGB());
        }
        g.setColor(MOON_SILVER); g.drawRect(0, 0, W - 1, W - 1);
        g.setColor(c(30, 44, 80)); g.drawRect(1, 1, W - 3, W - 3);
    }

    /** A crescent: the disc of radius r at (x, y), less the same disc shifted by (dx, dy). */
    static void crescent(Graphics2D g, double x, double y, double r, double dx, double dy, Color col) {
        Area a = new Area(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
        a.subtract(new Area(new Ellipse2D.Double(x - r + dx, y - r + dy, r * 2, r * 2)));
        g.setColor(col); g.fill(a);
    }

    /** The Crescent Moon Halberd from its butt to its point: a lacquer shaft, a white tassel, a silver point, the crescent on its side. */
    static void halberd(Graphics2D g, BufferedImage img, double x0, double y0, double x1, double y1) {
        double dx = x1 - x0, dy = y1 - y0, len = Math.hypot(dx, dy), ux = dx / len, uy = dy / len, nx = -uy, ny = ux;
        double hx = x1 - ux * 7, hy = y1 - uy * 7;
        line(g, x0, y0, hx, hy, 1.6f, LACQUER);
        line(g, x0, y0, x0 + ux * 1.4, y0 + uy * 1.4, 1.8f, MOON_SILVER);
        // The tassel, the collar.
        line(g, hx - ux * 1.2, hy - uy * 1.2, hx - ux * 3 - nx * 2.4, hy - uy * 3 - ny * 2.4, 1.4f, MOON_WHITE);
        line(g, hx - nx * 1.3, hy - ny * 1.3, hx + nx * 1.3, hy + ny * 1.3, 1.2f, MOON_SILVER);
        // The point.
        Path2D head = new Path2D.Double();
        head.moveTo(hx, hy); head.lineTo(hx + ux * 2 + nx * 1.2, hy + uy * 2 + ny * 1.2); head.lineTo(x1, y1);
        head.lineTo(hx + ux * 2 - nx * 1.2, hy + uy * 2 - ny * 1.2); head.closePath();
        g.setColor(MOON_SILVER); g.fill(head);
        line(g, hx + ux, hy + uy, x1 - ux, y1 - uy, 0.6f, MOON_WHITE);
        // The crescent blade on its side, its edge glowing.
        double cx = hx + ux * 1.2 + nx * 3.0, cy = hy + uy * 1.2 + ny * 3.0;
        crescent(g, cx, cy, 3.4, -nx * 1.6, -ny * 1.6, MOON_BLUE);
        crescent(g, cx + nx * 0.4, cy + ny * 0.4, 3.0, -nx * 1.9, -ny * 1.9, MOON_WHITE);
    }

    /** A small standing figure (a body, a head), its feet at (x, y), in one colour. */
    static void figureAt(Graphics2D g, double x, double y, Color col) {
        g.setColor(col);
        g.fill(new Ellipse2D.Double(x - 2, y - 13, 4, 4));
        g.fill(new Rectangle2D.Double(x - 2.5, y - 9, 5, 6));
        g.fill(new Rectangle2D.Double(x - 2.2, y - 3, 1.8, 3));
        g.fill(new Rectangle2D.Double(x + 0.4, y - 3, 1.8, 3));
    }

    static final Color WATER = c(110, 190, 255, 190), WATER_RIM = c(220, 240, 255), FOE = c(16, 20, 34);

    /** A reflection: a figure of translucent water with a bright rim. */
    static void reflection(Graphics2D g, double x, double y) {
        figureAt(g, x, y, c(220, 240, 255, 110));
        figureAt(g, x, y - 0.4, WATER);
    }

    static void moon(Graphics2D g, BufferedImage img, int i) {
        moonBack(img, g);
        switch (i) {
            case 0 -> { // Twin Reflection: the foe between the halberd and a reflection striking it from behind.
                figureAt(g, 16, 25, FOE);
                reflection(g, 25, 25);
                line(g, 24, 13, 18, 18, 1.2f, WATER_RIM);
                halberd(g, img, 1, 22, 12, 13);
                g.setColor(c(244, 251, 255, 160)); g.draw(new Arc2D.Double(10, 8, 12, 12, 30, 120, Arc2D.OPEN));
            }
            case 1 -> { // Shattered Mirror: a reflection bursting into shards.
                reflection(g, 16, 25);
                double[][] shards = {{5, 8}, {27, 7}, {4, 20}, {28, 21}, {9, 3}, {23, 3}, {16, 2}, {3, 14}, {29, 14}};
                for (double[] p : shards) {
                    Path2D sh = new Path2D.Double();
                    sh.moveTo(p[0], p[1]); sh.lineTo(p[0] + 2.4, p[1] + 1); sh.lineTo(p[0] + 0.6, p[1] + 2.8); sh.closePath();
                    g.setColor(MOON_BLUE); g.fill(sh);
                    line(g, 16, 16, p[0] + 1, p[1] + 1, 0.5f, c(220, 240, 255, 90));
                }
                disc(g, 16, 16, 3, c(244, 251, 255, 200));
            }
            case 2 -> { // Swap: the bearer and a reflection, two curved arrows between them.
                figureAt(g, 8, 26, LACQUER);
                line(g, 11, 14, 11, 26, 1.2f, MOON_SILVER);
                reflection(g, 24, 26);
                g.setColor(MOON_WHITE); g.setStroke(new BasicStroke(1.2f));
                g.draw(new Arc2D.Double(8, 4, 16, 10, 20, 140, Arc2D.OPEN));
                g.draw(new Arc2D.Double(8, 12, 16, 8, 200, 140, Arc2D.OPEN));
                line(g, 21, 6, 23, 8, 1.2f, MOON_WHITE); line(g, 23, 8, 20.5, 9, 1.2f, MOON_WHITE);
                line(g, 11, 18, 9, 16, 1.2f, MOON_WHITE); line(g, 9, 16, 11.5, 15, 1.2f, MOON_WHITE);
            }
            case 3 -> { // Hall of Mirrors: four reflections round the bearer.
                figureAt(g, 16, 22, LACQUER);
                reflection(g, 6, 16); reflection(g, 26, 16); reflection(g, 9, 30); reflection(g, 23, 30);
                g.setColor(c(159, 216, 255, 140)); g.setStroke(new BasicStroke(0.8f)); g.draw(new Ellipse2D.Double(3, 9, 26, 20));
            }
            case 4 -> { // Night of the Full Moon: a great moon, beams falling from it.
                disc(g, 16, 9, 7.5, c(159, 216, 255, 70));
                disc(g, 16, 9, 6, MOON_WHITE);
                disc(g, 14, 7, 1.4, c(196, 206, 220)); disc(g, 18.5, 11, 1.1, c(196, 206, 220)); disc(g, 13, 11.5, 0.8, c(196, 206, 220));
                g.setColor(c(159, 216, 255, 120));
                for (double x : new double[]{6, 16, 26}) g.fill(new Rectangle2D.Double(x - 1, 15, 2, 13));
                g.setColor(c(159, 216, 255, 200));
                for (double x : new double[]{6, 16, 26}) g.fill(new Ellipse2D.Double(x - 3, 27, 6, 2.4));
                px(img, 30, 30, c(220, 60, 60));
            }
            default -> { // Mirror Image: a figure dodging aside, its reflection left where it stood.
                reflection(g, 12, 27);
                figureAt(g, 23, 27, LACQUER);
                for (int k = 0; k < 3; k++) line(g, 15, 18 + k * 3, 20, 18 + k * 3, 0.8f, c(159, 216, 255, 150));
                g.setColor(c(110, 190, 255, 120)); g.fill(new Ellipse2D.Double(6, 26, 12, 3));
            }
        }
    }
}
