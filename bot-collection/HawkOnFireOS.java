package TeamBlackHole.Samples;
import robocode.*;
import robocode.util.Utils;
import java.awt.Color;
import java.util.Hashtable;
import java.util.Enumeration;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;

//  HawkOnFire - a robot by rozu

public class HawkOnFireOS extends AdvancedRobot
{
        static Hashtable enemies = new Hashtable();
        static microEnemy target;
        static Point2D.Double nextDestination;
        static Point2D.Double lastPosition;
        static Point2D.Double myPos;
        static double myEnergy;
        
//- run -------------------------------------------------------------------------------------------------------------------------------------
        public void run()
        {
                setColors(Color.black,Color.red,Color.black);
                setAdjustGunForRobotTurn(true);
                setAdjustRadarForGunTurn(true);
                
                setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
                
                nextDestination = lastPosition = myPos = new Point2D.Double(getX(), getY());
                target = new microEnemy();
                
                do {
                        
                        myPos = new Point2D.Double(getX(),getY());
                        myEnergy = getEnergy();
                        
                        // wait until you have scanned all other bots. this should take around 7 to 9 ticks.
                        if(target.live && getTime()>9) {
                                doMovementAndGun();
                        }
                        
                        execute();
                        
                } while(true);
        }
        
//- stuff -----------------------------------------------------------------------------------------------------------------------------------
        public void doMovementAndGun() {
                
                double distanceToTarget = myPos.distance(target.pos);
                
        //**** gun ******************//
                // HeadOnTargeting there's nothing I can say about this
                if(getGunTurnRemaining() == 0 && myEnergy > 1) {
                        setFire( Math.min(Math.min(myEnergy/6d, 1300d/distanceToTarget), target.energy/3d) );
                }
                
                setTurnGunRightRadians(Utils.normalRelativeAngle(calcAngle(target.pos, myPos) - getGunHeadingRadians()));
                
        //**** move *****************//
                double distanceToNextDestination = myPos.distance(nextDestination);
                
                //search a new destination if I reached this one
                if(distanceToNextDestination < 15) {
                        
                        // there should be better formulas then this one but it is basically here to increase OneOnOne performance. with more bots
                        // addLast will mostly be 1
                        double addLast = 1 - Math.rint(Math.pow(Math.random(), getOthers()));
                        
                        Rectangle2D.Double battleField = new Rectangle2D.Double(30, 30, getBattleFieldWidth() - 60, getBattleFieldHeight() - 60);
                        Point2D.Double testPoint;
                        int i=0;
                        
                        do {
                                //      calculate the testPoint somewhere around the current position. 100 + 200*Math.random() proved to be good if there are
                                //      around 10 bots in a 1000x1000 field. but this needs to be limited this to distanceToTarget*0.8. this way the bot wont
                                //      run into the target (should mostly be the closest bot) 
                                testPoint = calcPoint(myPos, Math.min(distanceToTarget*0.8, 100 + 200*Math.random()), 2*Math.PI*Math.random());
                                if(battleField.contains(testPoint) && evaluate(testPoint, addLast) < evaluate(nextDestination, addLast)) {
                                        nextDestination = testPoint;
                                }
                        } while(i++ < 200);
                        
                        lastPosition = myPos;
                        
                } else {
                        
                // just the normal goTo stuff
                        double angle = calcAngle(nextDestination, myPos) - getHeadingRadians();
                        double direction = 1;
                        
                        if(Math.cos(angle) < 0) {
                                angle += Math.PI;
                                direction = -1;
                        }
                        
                        setAhead(distanceToNextDestination * direction);
                        setTurnRightRadians(angle = Utils.normalRelativeAngle(angle));
                        // hitting walls isn't a good idea, but HawkOnFire still does it pretty often
                        setMaxVelocity(Math.abs(angle) > 1 ? 0 : 8d);
                        
                }
        }
        
//- eval position ---------------------------------------------------------------------------------------------------------------------------
        public static double evaluate(Point2D.Double p, double addLast) {
                // this is basically here that the bot uses more space on the battlefield. In melee it is dangerous to stay somewhere too long.
                double eval = addLast*0.08/p.distanceSq(lastPosition);
                
                Enumeration enumVar = enemies.elements();
                while (enumVar.hasMoreElements()) {
                        microEnemy en = (microEnemy)enumVar.nextElement();
                        // this is the heart of HawkOnFire. So I try to explain what I wanted to do:
                        // -    Math.min(en.energy/myEnergy,2) is multiplied because en.energy/myEnergy is an indicator how dangerous an enemy is
                        // -    Math.abs(Math.cos(calcAngle(myPos, p) - calcAngle(en.pos, p))) is bigger if the moving direction isn't good in relation
                        //              to a certain bot. it would be more natural to use Math.abs(Math.cos(calcAngle(p, myPos) - calcAngle(en.pos, myPos)))
                        //              but this wasn't going to give me good results
                        // -    1 / p.distanceSq(en.pos) is just the normal anti gravity thing
                        if(en.live) {
                                eval += Math.min(en.energy/myEnergy,2) * 
                                                (1 + Math.abs(Math.cos(calcAngle(myPos, p) - calcAngle(en.pos, p)))) / p.distanceSq(en.pos);
                        }
                }
                return eval;
        }
        
//- scan event ------------------------------------------------------------------------------------------------------------------------------
        public void onScannedRobot(ScannedRobotEvent e)
        {
                microEnemy en = (microEnemy)enemies.get(e.getName());
                
                if(en == null){
                        en = new microEnemy();
                        enemies.put(e.getName(), en);
                }
                
                en.energy = e.getEnergy();
                en.live = true;
                en.pos = calcPoint(myPos, e.getDistance(), getHeadingRadians() + e.getBearingRadians());
                
                // normal target selection: the one closer to you is the most dangerous so attack him
                if(!target.live || e.getDistance() < myPos.distance(target.pos)) {
                        target = en;
                }
                
                // locks the radar if there is only one opponent left
                if(getOthers()==1)      setTurnRadarLeftRadians(getRadarTurnRemainingRadians());
        }
        
//- minor events ----------------------------------------------------------------------------------------------------------------------------
        public void onRobotDeath(RobotDeathEvent e) {
                ((microEnemy)enemies.get(e.getName())).live = false;
        }
        
//- math ------------------------------------------------------------------------------------------------------------------------------------
        private static Point2D.Double calcPoint(Point2D.Double p, double dist, double ang) {
                return new Point2D.Double(p.x + dist*Math.sin(ang), p.y + dist*Math.cos(ang));
        }
        
        private static double calcAngle(Point2D.Double p2,Point2D.Double p1){
                return Math.atan2(p2.x - p1.x, p2.y - p1.y);
        }
        
//- microEnemy ------------------------------------------------------------------------------------------------------------------------------
        public class microEnemy {
                public Point2D.Double pos;
                public double energy;
                public boolean live;
        }
}

