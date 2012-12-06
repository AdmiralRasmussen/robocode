   package jk.mega;

   import robocode.*;
   import robocode.util.Utils;
   import java.awt.geom.*;     // for Point2D's
   import java.lang.*;         // for Double and Integer objects
   import java.util.*; // for collection of waves
   import java.awt.Color;

    public class DrussGT extends AdvancedRobot {
      static final int MAX_GF = 0;
      static final int ORBIT = 1;
    
      static final int BINS = 151;
      static final int MIDDLE_BIN = (BINS - 1)/2;
      static ArrayList statBuffers = new ArrayList();
      
      public Point2D.Double _myLocation = new Point2D.Double();     // our bot's location
      public Point2D.Double _enemyLocation = new Point2D.Double();  // enemy bot's location
      public double direction = 1;
   
      public ArrayList _distances;
      public ArrayList _lateralVelocitys;
      public ArrayList _advancingVelocitys;
      public ArrayList _enemyWaves;
      public ArrayList _surfDirections;
      public ArrayList _surfAbsBearings;
   	
      private static double BULLET_POWER = 1.9;
   
      private static double lateralDirection;
      private static double lastEnemyVelocity;
   
    // We must keep track of the enemy's energy level to detect EnergyDrop,
    // indicating a bullet is fired
      public static double _oppEnergy = 100.0;
   
    // This is a rectangle that represents an 800x600 battle field,
    // used for a simple, iterative WallSmoothing method (by Kawigi).
    // If you're not familiar with WallSmoothing, the wall stick indicates
    // the amount of space we try to always have on either end of the tank
    // (extending straight out the front or back) before touching a wall.
      public static Rectangle2D.Double _fieldRect
        = new java.awt.geom.Rectangle2D.Double(18, 18, 764, 564);
      public  ArrayList goToTargets;
      public  Point2D.Double lastGoToPoint;
      public static double WALL_STICK = 160;
      public long lastScanTime = 0;
      public static int _enemyHits;
      public boolean surfStatsChanged;
      public double enemyGunHeat;
      public static double bestDistance = 400;
      public static double flattenerWeight = 0;
      
      static RaikoGun raikoGun;
      static WaylanderGun waylanderGun;
   
       public void run() {
         if(getRoundNum() == 0){
         
            loadStatBuffers();
            
             
         	//preloaded HOT hit
            SingleBuffer sb = (SingleBuffer)((StatBuffer)statBuffers.get(0)).getStats(0,0,0,0,0,0);
            for (int x = 1; x < BINS; x++) {
               double increment = 1 / (Math.pow((MIDDLE_BIN - x)/5.0, 2) + 1);
               sb.bins[x] = increment;
            }
            sb.bins[0] = 1;
            
            _fieldRect = new java.awt.geom.Rectangle2D.Double(18, 18, getBattleFieldWidth() - 36, getBattleFieldHeight() - 36); 
         }
         raikoGun = new RaikoGun(this);
         waylanderGun = new WaylanderGun(this);
         
       
         setColors(Color.BLUE, Color.BLACK, Color.YELLOW);
         lateralDirection = 1;
         lastEnemyVelocity = 0;
      
         _lateralVelocitys = new ArrayList();
         _advancingVelocitys = new ArrayList();
         _enemyWaves = new ArrayList();
         _surfDirections = new ArrayList();
         _surfAbsBearings = new ArrayList();
         _distances = new ArrayList();
      
         setAdjustGunForRobotTurn(true);
         setAdjustRadarForGunTurn(true);
         setAdjustRadarForRobotTurn(true);
      
         do {
            // basic mini-radar code
            //if(_oppEnergy < 10)
               //doSurfing();
            if(Math.abs(getRadarTurnRemaining())  < 0.000001 && getOthers() > 0){
               if(getTime() > 9 )
                  System.out.println("Lost radar lock");
               setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
            }
         		
            if(lastScanTime + 1 < getTime()){
               //System.out.println("No scans, forcing surfing");
               _myLocation = new Point2D.Double(getX(), getY());
               updateWaves();
               doSurfing(); 
            }
            execute();
            
         } while (true);
      }
   
       public void onScannedRobot(ScannedRobotEvent e) {
         lastScanTime = getTime();
         _myLocation = new Point2D.Double(getX(), getY());
         double lateralVelocity = getVelocity()*Math.sin(e.getBearingRadians());
         double advancingVelocity = -getVelocity()*Math.cos(e.getBearingRadians());
         double absBearing = e.getBearingRadians() + getHeadingRadians();
         if(lateralVelocity > 0)
            lateralDirection = 1;
         else if(lateralVelocity < 0)
            lateralDirection = -1;
       
         setTurnRadarRightRadians(Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians()) * 2);
      
         
         _surfDirections.add(0,new Integer((int)lateralDirection));
         _surfAbsBearings.add(0, new Double(absBearing + Math.PI));
         _lateralVelocitys.add(0, new Integer((int)Math.abs(Math.round(lateralVelocity))));
         _advancingVelocitys.add(0, new Integer((int)Math.round(advancingVelocity)));
         _distances.add(0, new Double(e.getDistance()));
         
      
         enemyGunHeat = Math.max(0.0, enemyGunHeat - getGunCoolingRate());
         
         double bulletPower = _oppEnergy - e.getEnergy();
         if (bulletPower < 3.01 && bulletPower > 0.00001
            && _surfDirections.size() > 2
          	&& enemyGunHeat  < 0.01
         	) {
            enemyGunHeat = 1 + bulletPower/5;
            EnemyWave ew = new EnemyWave();
            ew.fireTime = getTime() - 1;
            ew.bulletVelocity = bulletVelocity(bulletPower);
            ew.distanceTraveled = ew.bulletVelocity;
            ew.direction = ((Integer)_surfDirections.get(2)).intValue();
            ew.directAngle = ((Double)_surfAbsBearings.get(2)).doubleValue();
            ew.fireLocation = (Point2D.Double)_enemyLocation.clone(); // last tick
            
         
           //  ArrayList forwardPoints = predictPositions(ew,ew.direction, MAX_GF);
         //    ArrayList reversePoints = predictPositions(ew,-ew.direction, MAX_GF);
         // 
         //    Point2D.Double maxF = (Point2D.Double)forwardPoints.get(forwardPoints.size() - 1);
         //    Point2D.Double maxR = (Point2D.Double)reversePoints.get(reversePoints.size() - 1);
         // 
         //    ew.maxFangle = Math.abs(Utils.normalRelativeAngle(ew.directAngle - absoluteBearing(_enemyLocation, maxF)));
         //    ew.maxRangle = Math.abs(Utils.normalRelativeAngle(ew.directAngle - absoluteBearing(_enemyLocation, maxR)));
            ew.maxFangle = maxEscapeAngle(ew.bulletVelocity);
            ew.maxRangle = maxEscapeAngle(ew.bulletVelocity);
         
            setSegments(ew);
         
            _enemyWaves.add(ew);
         }
               
         _oppEnergy = e.getEnergy();
      
        // update after EnemyWave detection, because that needs the previous
        // enemy location as the source of the wave
         _enemyLocation = project(_myLocation, absBearing, e.getDistance());
      
         updateWaves();
         
         doSurfing(); 
      
      
         // double radarTurn = Math.sin(absBearing - getRadarHeadingRadians());
         // radarTurn += Math.signum(radarTurn)*Math.atan(25/e.getDistance());
         // double MAX_RADAR_TURN = Math.PI/4 - Math.PI/8 - Math.PI/18;
         // radarTurn = limit(-MAX_RADAR_TURN, radarTurn, MAX_RADAR_TURN);
      //    
         // setTurnRadarRightRadians(radarTurn );
         
        raikoGun.onScannedRobot(e);
        if(getRoundNum() < 5)  
        waylanderGun.onScannedRobot(e);
      	
      	
      }
       public void setSegments(EnemyWave ew){
      
                  
         int prevLatVel = 0;
         try{prevLatVel = ((Integer)_lateralVelocitys.get(3)).intValue();}
             catch(Exception ex){}
         int lastLatVel = ((Integer)_lateralVelocitys.get(2)).intValue();
            
         int accel = 0;
         if(lastLatVel > prevLatVel)
            accel = 1;
         if(lastLatVel < prevLatVel)
            accel = 2;  
         	
         double distance = _myLocation.distance(_enemyLocation);
         try{distance = ((Double)_distances.get(2)).doubleValue();}
             catch(Exception ex){}
                
         double advVel = ((Integer)_advancingVelocitys.get(2)).intValue();
            
         double BFT = distance/ew.bulletVelocity;   
         		 
         double tsdirchange = 0;
         for(int i = 3; i < _surfDirections.size(); i++)
            if(((Integer)_surfDirections.get(i-1)).intValue() == ((Integer)_surfDirections.get(i)).intValue())
               tsdirchange++;
            else 
               break;
            
         		 
         double tsvchange = 0;
         for(int i = 3; i < _lateralVelocitys.size(); i++)
            if(((Integer)_lateralVelocitys.get(i-1)).intValue() <= ((Integer)_lateralVelocitys.get(i)).intValue())
               tsvchange++;
            else 
               break;
            
         double dl20 = 0;
         for(int i = 2; i < Math.min(22, _lateralVelocitys.size()); i++)
            dl20 += ((Integer)_lateralVelocitys.get(i)).intValue()*((Integer)_surfDirections.get(i)).intValue();
         dl20 = Math.abs(dl20);
         			
         	
         ew.allStats = new ArrayList();
         for(int i = 0; i < statBuffers.size(); i++)
            ew.allStats.add(((StatBuffer)statBuffers.get(i)).getStats(
                  lastLatVel,
                  distance, 
                  tsdirchange,
                  accel,
                  tsvchange,
                  dl20
                  ));
         
      }  
   	
       public void onBulletHit(BulletHitEvent e){
         double power = e.getBullet().getPower();
         double damage = 4*power;
         if(power > 1)
            damage += 2*(power - 1);
            
         _oppEnergy -= damage;
      
      }
       public void onDeath(DeathEvent e) {
         Vector v = getAllEvents();
         Iterator i = v.iterator();
         while(i.hasNext()){
            Object obj = i.next();
            if(obj instanceof HitByBulletEvent) {
               onHitByBullet((HitByBulletEvent) obj);
            }
         }
      }
      
       public void onSkippedTurn(SkippedTurnEvent e){
         System.out.println("Skipped TURN!!!");
      }
   
       public void updateWaves() {
         for (int x = 0; x < _enemyWaves.size(); x++) {
            EnemyWave ew = (EnemyWave)_enemyWaves.get(x);
         
            ew.distanceTraveled = (getTime() - ew.fireTime) * ew.bulletVelocity;
            if (ew.distanceTraveled >
                _myLocation.distance(ew.fireLocation) + 50) {
               _enemyWaves.remove(x);
               x--;
               
               // logHit(ew,_myLocation,flattenerWeight);
            	
            	
            }
            else if(Math.abs(ew.distanceTraveled - ew.bulletVelocity*2) < 0.001)
            //  && _myLocation.distance(_enemyLocation) < 300)
               surfStatsChanged = true;
         }
      }
       public void loadStatBuffers(){
         double[] empty = {};
       
         double[] velSlicesRough = {2,4,6};
         double[] velSlices = {1,3,5,7};
         double[] velSlicesFine = {1,2,3,4,5,6,7,8};
      
         double[] distSlicesRough = {200,400,600};
         double[] distSlices = {100, 300, 500, 700};
         double[] distSlicesFine = {100, 200, 300, 400, 500, 600, 700, 800};
         
         double[] tsdcSlicesRough = {10,20,30};
         double[] tsdcSlices = {7.5,15,22.5,30};
         double[] tsdcSlicesFine = {4,8,12,16,20,24,28};
         
         double[] accelSlices = {0.5,1.5};
         
         double[] tsvcSlicesRough = {6,15,30};
         double[] tsvcSlices = {4,8,16,30};
         double[] tsvcSlicesFine = {2,4,6,8,16,24,28};
         
         double[] dl20SlicesRough = {30,60,90};
         double[] dl20Slices = {20,40,60,80,100,115};
         double[] dl20SlicesFine = {15,30,45,60,75,90,105,120};
      	
         
         statBuffers.add(new StatBuffer(empty,empty,empty,empty,empty,empty));
         statBuffers.add(new StatBuffer(velSlicesRough,empty,empty,empty,empty,empty));
         statBuffers.add(new StatBuffer(velSlices,empty,empty,empty,empty,empty));
         statBuffers.add(new StatBuffer(velSlicesFine,empty,empty,empty,empty,empty));
         statBuffers.add(new StatBuffer(velSlicesRough,distSlicesRough,empty,empty,empty,empty));
         statBuffers.add(new StatBuffer(velSlices,distSlicesRough,empty,empty,empty,empty));
         statBuffers.add(new StatBuffer(velSlices,distSlices,empty,empty,empty,empty));
         statBuffers.add(new StatBuffer(velSlicesFine,distSlices,empty,empty,empty,empty));
         statBuffers.add(new StatBuffer(velSlicesFine,distSlicesFine,empty,empty,empty,empty));
         
         statBuffers.add(new StatBuffer(velSlicesRough,empty,tsdcSlicesRough,empty,empty,empty));
         statBuffers.add(new StatBuffer(velSlicesRough,empty,tsdcSlices,empty,empty,empty));
         statBuffers.add(new StatBuffer(velSlices,empty,tsdcSlices,empty,empty,empty));
         statBuffers.add(new StatBuffer(velSlicesRough,distSlicesRough,tsdcSlicesRough,empty,empty,empty));
         statBuffers.add(new StatBuffer(velSlices,distSlicesRough,tsdcSlicesRough,empty,empty,empty));
         statBuffers.add(new StatBuffer(velSlices,distSlices,tsdcSlicesRough,empty,empty,empty));
         statBuffers.add(new StatBuffer(velSlices,distSlices,tsdcSlices,empty,empty,empty));
         statBuffers.add(new StatBuffer(velSlicesFine,distSlices,tsdcSlices,empty,empty,empty));
         statBuffers.add(new StatBuffer(velSlicesFine,distSlicesFine,tsdcSlices,empty,empty,empty));
         statBuffers.add(new StatBuffer(velSlicesRough,distSlicesRough,tsdcSlicesFine,empty,empty,empty));
         statBuffers.add(new StatBuffer(velSlices,distSlicesRough,tsdcSlicesFine,empty,empty,empty));
         statBuffers.add(new StatBuffer(velSlices,distSlices,tsdcSlicesFine,empty,empty,empty));
         statBuffers.add(new StatBuffer(velSlicesFine,distSlicesRough,tsdcSlicesFine,empty,empty,empty));
         statBuffers.add(new StatBuffer(velSlicesFine,distSlices,tsdcSlicesFine,empty,empty,empty));
         statBuffers.add(new StatBuffer(velSlicesFine,distSlicesFine,tsdcSlicesFine,empty,empty,empty));
      	
         // statBuffers.add(new StatBuffer(velSlicesRough,distSlicesRough,empty,accelSlices,empty,empty));
         // statBuffers.add(new StatBuffer(velSlices,distSlicesRough,empty,accelSlices,empty,empty));
         // statBuffers.add(new StatBuffer(velSlices,distSlices,empty,accelSlices,empty,empty));
         // statBuffers.add(new StatBuffer(velSlicesFine,distSlices,empty,accelSlices,empty,empty));
         // statBuffers.add(new StatBuffer(velSlicesFine,distSlicesFine,empty,accelSlices,empty,empty));
         statBuffers.add(new StatBuffer(velSlices,distSlices,tsdcSlicesRough,accelSlices,empty,empty));
         statBuffers.add(new StatBuffer(velSlices,distSlices,tsdcSlices,accelSlices,empty,empty));
         statBuffers.add(new StatBuffer(velSlicesRough,distSlicesRough,tsdcSlicesFine,accelSlices,empty,empty));
         statBuffers.add(new StatBuffer(velSlices,distSlicesRough,tsdcSlicesFine,accelSlices,empty,empty));
         statBuffers.add(new StatBuffer(velSlices,distSlices,tsdcSlicesFine,accelSlices,empty,empty));
         statBuffers.add(new StatBuffer(velSlicesFine,distSlicesRough,tsdcSlicesFine,accelSlices,empty,empty));
         statBuffers.add(new StatBuffer(velSlicesFine,distSlices,tsdcSlicesFine,accelSlices,empty,empty));
      
         statBuffers.add(new StatBuffer(velSlicesRough,distSlicesRough,empty,empty,tsvcSlicesRough,empty));
         statBuffers.add(new StatBuffer(velSlices,distSlicesRough,empty,empty,tsvcSlicesRough,empty));
         statBuffers.add(new StatBuffer(velSlicesRough,distSlicesRough,empty,empty,tsvcSlices,empty));
         statBuffers.add(new StatBuffer(velSlices,distSlicesRough,empty,empty,tsvcSlices,empty));
         statBuffers.add(new StatBuffer(velSlices,distSlices,empty,empty,tsvcSlices,empty));
         statBuffers.add(new StatBuffer(velSlices,distSlices,empty,empty,tsvcSlicesFine,empty));
         statBuffers.add(new StatBuffer(velSlicesFine,distSlices,empty,empty,tsvcSlicesFine,empty));
         statBuffers.add(new StatBuffer(velSlicesFine,distSlicesFine,empty,empty,tsvcSlicesFine,empty));
      //    statBuffers.add(new StatBuffer(velSlicesRough,distSlicesRough,tsdcSlicesRough,empty,tsvcSlicesRough,empty));
      //    statBuffers.add(new StatBuffer(velSlices,distSlicesRough,tsdcSlicesRough,empty,tsvcSlicesRough,empty));
      //    statBuffers.add(new StatBuffer(velSlices,distSlices,tsdcSlices,empty,tsvcSlices,empty));
      //    statBuffers.add(new StatBuffer(velSlices,distSlices,tsdcSlices,accelSlices,tsvcSlices,empty));
          
         statBuffers.add(new StatBuffer(velSlicesRough,empty,empty,empty,empty,dl20SlicesRough));
         statBuffers.add(new StatBuffer(velSlicesRough,empty,empty,empty,empty,dl20Slices));
         statBuffers.add(new StatBuffer(velSlices,empty,empty,empty,empty,dl20Slices));
         statBuffers.add(new StatBuffer(velSlicesRough,distSlicesRough,empty,empty,empty,dl20SlicesRough));
         statBuffers.add(new StatBuffer(velSlices,distSlicesRough,empty,empty,empty,dl20SlicesRough));
         statBuffers.add(new StatBuffer(velSlices,distSlices,empty,empty,empty,dl20SlicesRough));
         statBuffers.add(new StatBuffer(velSlices,distSlices,empty,empty,empty,dl20Slices));
         statBuffers.add(new StatBuffer(velSlicesFine,distSlices,empty,empty,empty,dl20Slices));
         statBuffers.add(new StatBuffer(velSlicesFine,distSlicesFine,empty,empty,empty,dl20Slices));
         statBuffers.add(new StatBuffer(velSlicesRough,distSlicesRough,empty,empty,empty,dl20SlicesFine));
         statBuffers.add(new StatBuffer(velSlices,distSlicesRough,empty,empty,empty,dl20SlicesFine));
         statBuffers.add(new StatBuffer(velSlices,distSlices,empty,empty,empty,dl20SlicesFine));
         statBuffers.add(new StatBuffer(velSlicesFine,distSlicesRough,empty,empty,empty,dl20SlicesFine));
         statBuffers.add(new StatBuffer(velSlicesFine,distSlices,empty,empty,empty,dl20SlicesFine));
         statBuffers.add(new StatBuffer(velSlicesFine,distSlicesFine,empty,empty,empty,dl20SlicesFine));
      	
      
      }
   
   
   
   
   
       public EnemyWave getClosestSurfableWave() {
         double closestDistance = Double.POSITIVE_INFINITY; 
         EnemyWave surfWave = null;
      
         for (int x = 0; x < _enemyWaves.size(); x++) {
            EnemyWave ew = (EnemyWave)_enemyWaves.get(x);
            double distance = _myLocation.distance(ew.fireLocation)
                - ew.distanceTraveled;
         
            if (distance > ew.bulletVelocity && distance < closestDistance) {
               surfWave = ew;
               closestDistance = distance;
            }
         }
      
         return surfWave;
      }
   
    // Given the EnemyWave that the bullet was on, and the point where we
    // were hit, calculate the index into our stat array for that factor.
       public static int getFactorIndex(EnemyWave ew, Point2D.Double targetLocation) {
         double offsetAngle = Utils.normalRelativeAngle(absoluteBearing(ew.fireLocation, targetLocation)
            - ew.directAngle)*ew.direction;
            
         double escapeAngle = offsetAngle > 0?ew.maxFangle:ew.maxRangle;
      		
         double factor = offsetAngle / escapeAngle;
      
         return (int)limit(0,
            (factor * ((BINS - 1) / 2)) + ((BINS - 1) / 2),
            BINS - 1);
      }
   
    // Given the EnemyWave that the bullet was on, and the point where we
    // were hit, update our stat array to reflect the danger in that area.
       public void logHit(EnemyWave ew, Point2D.Double targetLocation, double weight) {
         int index = getFactorIndex(ew, targetLocation);
      
         // int x = index;
         for(int i = 0; i < ew.allStats.size(); i++){
            SingleBuffer sb = (SingleBuffer)ew.allStats.get(i);
           
            for (int x = 1; x < BINS; x++) {
               double increment = 1 / (Math.pow((index - x)/5.0, 2) + 1);
               sb.bins[x] = rollingAvg(sb.bins[x], increment, sb.rollingDepth, weight);
            }
          
            sb.bins[0] = limit(0,sb.bins[0] + 1,3);  
         	
         }
         
         for(int i = 0, k = _enemyWaves.size(); i < k; i++)
            ((EnemyWave)_enemyWaves.get(i)).bestBins = null;
            
         surfStatsChanged = true;
      	
      }
       static double rollingAvg(double value, double newEntry, double depth, double weighting ) {
         return (value * depth + newEntry * weighting)/(depth + weighting);
      } 
    /**
    * Bell curve smoother... also know as gaussian smooth or normal distrabution
    * Credits go to Chase-san
    * @param x Current Position
    * @param c Center (current index your adding)
    * @param w Width (number of binIndexes)
    * @return value of a bellcurve
    */ 
      // public static final double smoothingModifier = 5;
       // public static double bellcurve(int x, int c, int w) {
         // int diff = Math.abs(c - x);
         // double binsmooth = smoothingModifier/w;
      // 
      // //I suppose technically you could also use Math.exp(-(binsmooth*binsmooth*diff*diff)/2.0);
         // return  Math.exp(-(binsmooth*binsmooth*diff*diff)/2.0);
      //    // return Math.pow(Math.E, -(binsmooth*binsmooth*diff*diff)/2.0);
      // }
       public void onBulletHitBullet(BulletHitBulletEvent e){
      
         if (!_enemyWaves.isEmpty()) {
            Point2D.Double hitBulletLocation = new Point2D.Double(
                e.getBullet().getX(), e.getBullet().getY());
            EnemyWave hitWave = null;
         
            // look through the EnemyWaves, and find one that could've hit the bullet
            hitWave = getCollisionWave(hitBulletLocation,e.getHitBullet().getPower());
         
            if (hitWave != null) {
               logHit(hitWave, hitBulletLocation,1);
                // We can remove this wave now, of course.
               _enemyWaves.remove(_enemyWaves.lastIndexOf(hitWave));
               
            }
         }
      
      
      }
       public void onHitByBullet(HitByBulletEvent e) {
        // If the _enemyWaves collection is empty, we must have missed the
        // detection of this wave somehow.
         if (!_enemyWaves.isEmpty()) {
            Bullet bullet = e.getBullet();
            Point2D.Double hitBulletLocation = new Point2D.Double(
                e.getBullet().getX(), e.getBullet().getY());
            EnemyWave hitWave = null;
         
            // look through the EnemyWaves, and find one that could've hit us.
            hitWave = getCollisionWave(_myLocation,e.getBullet().getPower());
            if (hitWave != null) {
               // hitBulletLocation = 
                  // project(hitWave.fireLocation, bullet.getHeading(), hitWave.distanceTraveled);
            
               logHit(hitWave, hitBulletLocation,1);
            
                // We can remove this wave now, of course.
               _enemyWaves.remove(_enemyWaves.lastIndexOf(hitWave));
            }
         }
         _enemyHits++;
         
         _oppEnergy += e.getBullet().getPower()*3;
      }
       EnemyWave getCollisionWave(Point2D.Double point, double bulletPower){
         for (int x = 0; x < _enemyWaves.size(); x++) {
            EnemyWave ew = (EnemyWave)_enemyWaves.get(x);
            
            if (Math.abs(ew.distanceTraveled -
                    point.distance(ew.fireLocation)) < 50
                    && Math.round(bulletVelocity(bulletPower) * 10)
                       == Math.round(ew.bulletVelocity * 10)) {
               return ew;
              
            }
         }
         
         return null;
      }
   
    // CREDIT: mini sized predictor from Apollon, by rozu
    // http://robowiki.net?Apollon
       public ArrayList predictPositions(EnemyWave surfWave, int direction, int MODE) {
         Point2D.Double predictedPosition = new Point2D.Double(getX(), getY());
         ArrayList positions = new ArrayList();
      
         double predictedVelocity = getVelocity();
         double predictedHeading = getHeadingRadians();
         double maxTurning, moveAngle, prefOffset, moveDir;
      
         int counter = 0; // number of ticks in the future
         boolean intercepted = false;
      
         do {
         //keeps a distance of 500
            //prefOffset = Math.PI/2;//(0.00154*surfWave.fireLocation.distance(predictedPosition) + 0.8);
            double enemyDistance = _enemyLocation.distance(predictedPosition);
         
            prefOffset = Math.PI/2;// - 1 + limit(350,enemyDistance, 800)/600;
            double absBearing;
            // if(enemyDistance > 300)
               // absBearing = absoluteBearing(
                  // // _enemyLocation
                  // surfWave.fireLocation
                  // ,
                  // //  predictedPosition
                  // _myLocation    
                  // );
            // else {
            absBearing = absoluteBearing(
                //   _enemyLocation
                  surfWave.fireLocation
                  ,
                   
                  MODE == MAX_GF? _myLocation :
               	 
               	 predictedPosition   
                  );
            prefOffset = Math.PI/2 - 1 + limit(200,enemyDistance,900)/400;
            // }
         
            moveAngle =
                wallSmoothing(predictedPosition, absBearing+ (direction * prefOffset), direction)
                - predictedHeading;
            moveDir = 1;
         
            if(Math.cos(moveAngle) < 0) {
               moveAngle += Math.PI;
               moveDir = -1;
            }
         
            moveAngle = Utils.normalRelativeAngle(moveAngle);
         
         // maxTurning is built in like this, you can't turn more then this in one tick
            maxTurning = Math.toRadians(10 - 0.75*Math.abs(predictedVelocity));
            
            predictedHeading = Utils.normalRelativeAngle(predictedHeading
                + limit(-maxTurning, moveAngle, maxTurning));
         
         // this one is nice ;). if predictedVelocity and moveDir have
            // different signs you want to brake down
         // otherwise you want to accelerate (look at the factor "2")
            // predictedVelocity += (predictedVelocity * moveDir < 0 ? 2*moveDir : moveDir);
         	
            double velAddition = (predictedVelocity * moveDir < 0 ? 2*moveDir : moveDir);
            
            
            predictedVelocity = limit(-8, predictedVelocity + velAddition, 8);
            
         // calculate the new predicted position
            predictedPosition = project(predictedPosition, predictedHeading, predictedVelocity);
         
            positions.add(predictedPosition);
         
            counter++;
         
            if (predictedPosition.distance(surfWave.fireLocation)  <
                surfWave.distanceTraveled + (counter * surfWave.bulletVelocity)
                + surfWave.bulletVelocity
            
            	 ) {
               intercepted = true;
               if(positions.size() > 1 && MODE != MAX_GF){
                  positions.remove(positions.size() - 1);
                  
               	
                  if(positions.size() > 1){
                     predictedPosition = (Point2D.Double)positions.get(positions.size() - 1);
                  
                     Point2D.Double stopPosition = project(predictedPosition, predictedHeading, -2*Math.signum(predictedVelocity));
                     positions.set(positions.size() - 1, stopPosition);
                  }
               //else
                  // predictedPosition = project(predictedPosition, predictedHeading, -1*Math.signum(predictedVelocity));
               }
            
            }
            
           
               
               
         } while(!intercepted && counter < 500);
         
         return positions;
      }
   
       public Point2D.Double getBestPoint(EnemyWave surfWave){
         if(surfWave.safePoints == null || surfStatsChanged)
         {
            
         //This is coded up much more complexly than necessary
         //(I *could* just say 'go to the end point') so that I get
         //nice painting graphics. ;-). Actually, so that I follow
         //the exact path the PrecisePrediction followed.
         
         
            ArrayList forwardPoints = predictPositions(surfWave, 1, ORBIT);
            ArrayList reversePoints = predictPositions(surfWave, -1, ORBIT);
            int FminDangerIndex = 0;
            int RminDangerIndex = 0;
            double FminDanger = Double.POSITIVE_INFINITY;
            double RminDanger = Double.POSITIVE_INFINITY;
            for(int i = 0, k = forwardPoints.size(); i < k; i++){
               double thisDanger = checkDanger(surfWave, (Point2D.Double)(forwardPoints.get(i)));
               if(thisDanger <= FminDanger){
                  FminDangerIndex = i;
                  FminDanger = thisDanger;
               }
            }
            for(int i = 0, k = reversePoints.size(); i < k; i++){
               double thisDanger = checkDanger(surfWave, (Point2D.Double)(reversePoints.get(i)));
               if(thisDanger <= RminDanger){
                  RminDangerIndex = i;
                  RminDanger = thisDanger;
               }
            }
            ArrayList bestPoints;
            int ticksAhead;
            int safeDirection;
            if(FminDanger < RminDanger ){
               bestPoints = forwardPoints;
               ticksAhead = FminDangerIndex;
               safeDirection = 1;
            }
            else {
               bestPoints = reversePoints;
               ticksAhead = RminDangerIndex;
               safeDirection = -1;
            }
            Point2D.Double bestPoint = (Point2D.Double)bestPoints.get(ticksAhead);
         //  if(ticksAhead == bestPoints.size() - 1
         //    && ticksAhead > 0){
         //       Point2D.Double previousPoint = (Point2D.Double)bestPoints.get(ticksAhead - 1);
         //       double absBearing =absoluteBearing(previousPoint, bestPoint);
         //       double turn =  Utils.normalRelativeAngle(wallSmoothing(bestPoint,absBearing, safeDirection) - absBearing);
         //       double maxTurn = Math.toRadians(10 - 0.75*8);
         //       turn = limit(-maxTurn, turn , maxTurn);
         //       Point2D.Double destPoint = project(bestPoint, absBearing + turn, 28);
         //       // if(_fieldRect.contains(destPoint))
         //       bestPoints.add(destPoint);
         //       // else
         //       // bestPoints.remove(ticksAhead);
         //    }
         //    else
         //    {
         //      // //  Point2D.Double bestPoint = (Point2D.Double)bestPoints.get(ticksAhead);
            
            while(bestPoints.indexOf(bestPoint) != -1)
               bestPoints.remove(bestPoints.size() - 1);
            bestPoints.add(bestPoint);
               
          //   }
         	
            surfWave.safePoints = bestPoints;
            goToTargets = bestPoints;
            surfStatsChanged = false;
            
         	//debugging - should always be on top of the last point
            bestPoints.add(0,new Point2D.Double(getX(), getY()));
            
         }
         else
            if(surfWave.safePoints.size() > 1)
               surfWave.safePoints.remove(0);
         
         
         if(surfWave.safePoints.size() >= 1){
            for(int i = 0,k=surfWave.safePoints.size(); i < k; i++){
               Point2D.Double goToPoint = (Point2D.Double)surfWave.safePoints.get(i);
               if(goToPoint.distanceSq(_myLocation) > 20*20*1.0001)
               //if it's not 20 units away we won't reach max velocity
                  return goToPoint;
            }
         //if we don't find a point 20 units away, return the end point
            return (Point2D.Double)surfWave.safePoints.get(surfWave.safePoints.size() - 1);
               
         
         }
           
         return null;
      }
       public double checkDanger(EnemyWave surfWave, Point2D.Double checkPosition) {
         if(!_fieldRect.contains(checkPosition))
            return Double.POSITIVE_INFINITY;
       
         int index = getFactorIndex(surfWave,checkPosition);
         
      	//the maximum possible width - sqrt(40*40 + 40*40) = 25 - diagonal
         double botWidthAtEnd = 2*Math.atan(25/checkPosition.distance(surfWave.fireLocation));
      	
         double binWidth = maxEscapeAngle(surfWave.bulletVelocity)/MIDDLE_BIN;
         int botBinWidthAtEnd = (int)Math.round(botWidthAtEnd/binWidth);
      	
         if(surfWave.bestBins == null){
            surfWave.bestBins = new double[BINS];
            for(int i = 0; i < surfWave.allStats.size(); i++){
               SingleBuffer sb = (SingleBuffer)surfWave.allStats.get(i);
               for(int j = 1; j < BINS; j++)
                  surfWave.bestBins[j] += sb.bins[j]*sb.weight*sb.bins[0];
            }
         	
            normalize(surfWave.bestBins);
         }
        
      		 
         double thisDanger = getAverageDanger(surfWave.bestBins,index,botBinWidthAtEnd)*botWidthAtEnd;
         thisDanger /= Math.pow(checkPosition.distance(_enemyLocation), 0.5);
         thisDanger *= Math.pow(0.8,(_myLocation.distance(surfWave.fireLocation)-surfWave.distanceTraveled)/surfWave.bulletVelocity - 15);
         thisDanger*=(20 - surfWave.bulletVelocity);//bullet power
      		 
         _enemyWaves.remove(surfWave);
         EnemyWave nextWave = getClosestSurfableWave();
         
         double nextDanger;
         if(nextWave == null)
            nextDanger = 0;
         else
            nextDanger = checkDanger(nextWave, checkPosition);  
         
         _enemyWaves.add(surfWave);
         return thisDanger + nextDanger;
         
      
      		 
      }
       public boolean segEmpty(double[] bins, double threshHold){
         // boolean segEmpty = true;
         // for(int i = 1; i < BINS && segEmpty; i++)
            // segEmpty = (bins[i] == 0.0);
           
         // return segEmpty;
         return bins[0] < threshHold;
      }
       public static void normalize(double[] bins){
         double max = 0;
         for(int i = 1; i < bins.length; i++)
            if(bins[i] > max)
               max = bins[i];
               
      	
         if(max != 0.0)
            for(int i = 1; i < bins.length; i++)
               bins[i] = bins[i]/max;
      
      }
       public double getAverageDanger(double[] bins, int index, int botBinWidth){
         botBinWidth = (int)limit(2, botBinWidth, BINS - 1);
         double totalDanger = 0;
         
         int minIndex = Math.max(0,index - botBinWidth/2);
         int maxIndex = Math.min(BINS - 1, index + botBinWidth/2);
         for(int i = minIndex; i < maxIndex; i++)
            totalDanger += bins[i];
      	
         return totalDanger/(maxIndex - minIndex);
      
      }
   
       public void doSurfing() {
         EnemyWave surfWave = getClosestSurfableWave();
         double distance = _enemyLocation.distance(_myLocation);
         if (surfWave == null || distance < 80) { 
            //do 'away' movement  best distance of 600
            double absBearing = absoluteBearing(_myLocation, _enemyLocation);
            double headingRadians = getHeadingRadians();
            double stick = 140;//Math.min(160,distance);
            double  v2, offset = Math.max(Math.PI/3 + 0.021,Math.PI/2 + 1 - limit(200,distance,800)/400);
            
            while(!_fieldRect.
            contains(project(_myLocation,v2 = absBearing + direction*(offset -= 0.02), stick)
            ));
         
         
            if( offset < Math.PI/3 ||
            (
            _enemyLocation.distance(project(_myLocation, v2, 50)) < 50
            &&
            distance > 50
            )
            ) {
               direction = -direction;
               
               offset = Math.max(Math.PI/3 + 0.021,Math.PI/2 + 1 - limit(200,distance,800)/400);
            
               while(!_fieldRect.
               contains(project(_myLocation,v2 = absBearing + direction*(offset -= 0.02), stick)
               ));
            	
            }
            setAhead(50*Math.cos(v2 - headingRadians));
            setTurnRightRadians(Math.tan(v2 - headingRadians));
         
         }
         else {
            goTo(getBestPoint(surfWave));
            direction = -lateralDirection;
         }
      }
       private void goTo(Point2D.Double destination) {
         if(destination == null){
            if(lastGoToPoint != null)
               destination = lastGoToPoint;
            else
               return;
         }
         lastGoToPoint = destination;
         Point2D location = new Point2D.Double(getX(), getY());
         double distance = location.distance(destination);
         double angle = Utils.normalRelativeAngle(absoluteBearing(location, destination) - getHeadingRadians());
         if (Math.abs(angle) > Math.PI/2) {
            distance = -distance;
            if (angle > 0) {
               angle -= Math.PI;
            }
            else {
               angle += Math.PI;
            }
         }
         setTurnRightRadians(angle*Math.signum(Math.abs((int)distance)));
         setAhead(distance);
      }
       private double absoluteBearing(Point2D source, Point2D target) {
         return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
      }
    // This can be defined as an inner class if you want.
       class EnemyWave {
         Point2D.Double fireLocation;
         long fireTime;
         double bulletVelocity, directAngle, distanceTraveled;
         int direction;
         ArrayList allStats;
         double[] bestBins;
         ArrayList safePoints;
         double maxFangle,maxRangle;
      
          public EnemyWave() { }
      }
      
   
   
   //non-iterative wallsmoothing by Simonton - to save your CPUs
      public static final double HALF_PI = Math.PI / 2;
      public static final double WALKING_STICK = 160;
      public static final double WALL_MARGIN = 19;
      public static final double S = WALL_MARGIN;
      public static final double W = WALL_MARGIN;
      public static final double N = 600 - WALL_MARGIN;
      public static final double E = 800 - WALL_MARGIN;
   
    // angle = the angle you'd like to go if there weren't any walls
    // oDir  =  1 if you are currently orbiting the enemy clockwise
    //         -1 if you are currently orbiting the enemy counter-clockwise
    // returns the angle you should travel to avoid walls
       double wallSmoothing(Point2D.Double botLocation, double angle, int oDir) {
         // if(!_fieldRect.contains(project(botLocation,angle + Math.PI*(oDir + 1),WALKING_STICK))){
         angle = smoothWest(N - botLocation.y, angle - HALF_PI, oDir) + HALF_PI;
         angle = smoothWest(E - botLocation.x, angle + Math.PI, oDir) - Math.PI;
         angle = smoothWest(botLocation.y - S, angle + HALF_PI, oDir) - HALF_PI;
         angle = smoothWest(botLocation.x - W, angle, oDir);
         
         // for bots that could calculate an angle that is pointing pretty far
         // into a corner, these three lines may be necessary when travelling
         // counter-clockwise (since the smoothing above may have moved the 
         // walking stick into another wall)
         angle = smoothWest(botLocation.y - S, angle + HALF_PI, oDir) - HALF_PI;
         angle = smoothWest(E - botLocation.x, angle + Math.PI, oDir) - Math.PI;
         angle = smoothWest(N - botLocation.y, angle - HALF_PI, oDir) + HALF_PI;
         // }
         return angle;
      }
   
    // smooths agains the west wall
       static double smoothWest(double dist, double angle, int oDir) {
         if (dist < -WALKING_STICK * Math.sin(angle)) {
            return Math.acos(oDir * dist / WALKING_STICK) - oDir * HALF_PI;
         }
         return angle;
      }
   
   
    // CREDIT: from CassiusClay, by PEZ
    //   - returns point length away from sourceLocation, at angle
    // robowiki.net?CassiusClay
       public static Point2D.Double project(Point2D.Double sourceLocation, double angle, double length) {
         return new Point2D.Double(sourceLocation.x + Math.sin(angle) * length,
            sourceLocation.y + Math.cos(angle) * length);
      }
   
    // got this from RaikoMicro, by Jamougha, but I think it's used by many authors
    //  - returns the absolute angle (in radians) from source to target points
       public static double absoluteBearing(Point2D.Double source, Point2D.Double target) {
         return Math.atan2(target.x - source.x, target.y - source.y);
      }
   
       public static double limit(double min, double value, double max) {
         if(value > max)
            return max;
         if(value < min)
            return min;
       
         return value;
      }
   
       public static double bulletVelocity(double power) {
         return (20D - (3D*power));
      }
   
       public static double maxEscapeAngle(double velocity) {
         return Math.asin(8.0/velocity);
      }
   
       public void onPaint(java.awt.Graphics2D g) {
         g.setColor(Color.red);
         
         for(int i = 0; i < _enemyWaves.size(); i++){
            g.setColor(Color.red);
            EnemyWave w = (EnemyWave)(_enemyWaves.get(i));
            int radius = (int)(w.distanceTraveled);
            Point2D.Double center = w.fireLocation;
            if(radius - 40 < center.distance(_myLocation)){
               // g.drawOval((int)(center.x - radius ), (int)(center.y - radius), radius*2, radius*2);
               if(w.bestBins != null){
                  double MEA = maxEscapeAngle(w.bulletVelocity);
                  for(int j = 0; j < BINS; j++){   
                  
                     double thisDanger = w.bestBins[j];
                     g.setColor(Color.blue);
                     if(thisDanger > 0.1)
                        g.setColor(Color.green);
                     if(thisDanger > 0.3)
                        g.setColor(Color.yellow);
                     if(thisDanger > 0.6)
                        g.setColor(Color.orange);
                     if(thisDanger > 0.9)
                        g.setColor(Color.red);
                     Point2D.Double thisPoint;
                     if(j < MIDDLE_BIN){
                        thisPoint = project(center, w.directAngle + w.direction*(j - MIDDLE_BIN)/(double)MIDDLE_BIN*w.maxRangle, radius);
                     }
                     else
                        thisPoint = project(center, w.directAngle + w.direction*(j - MIDDLE_BIN)/(double)MIDDLE_BIN*w.maxFangle, radius);
                    
                     if(j == MIDDLE_BIN)
                        g.setColor(Color.pink);
                     g.drawOval((int)(thisPoint.x - 1),(int)(thisPoint.y - 1), 2, 2);
                  
                  
                  }
               }
            }
         }
         
         if(goToTargets != null){
            g.setColor(Color.green);
            for(int i = 0; i < goToTargets.size(); i++){
               Point2D.Double goToTarget = (Point2D.Double)goToTargets.get(i);
               g.drawOval((int)goToTarget.x - 2, (int)goToTarget.y - 2, 4,4);
            }
         }
         if(lastGoToPoint != null){
            g.setColor(Color.orange);
            g.drawOval((int)lastGoToPoint.x - 3, (int)lastGoToPoint.y - 3, 6,6);
            g.drawOval((int)lastGoToPoint.x - 4, (int)lastGoToPoint.y - 4, 8,8);
         }
      }
   }
    class SingleBuffer{
      double[] bins;
      double weight;
      double rollingDepth = 0.7;
   }  
	
	
    class StatBuffer{  
      static final int BINS = 151;
      
      double _weight;
      double rollingDepth;
      
      double [][][][][][][] stats;
   
      double[] velocitySlices;
      double[] distanceSlices;
      double[] timeSinceDirChangeSlices;
      double[] accelSlices;
      double[] timeSinceVelChangeSlices;
      double[] distLast20Slices;
   
   
       StatBuffer(
       double[] vSlices,
       double[] dSlices,
       double[] tsdcSlices,
       double[] accSlices,
       double[] tsvcSlices,
       double[] dl20Slices){
         velocitySlices = vSlices;
         distanceSlices = dSlices;
         timeSinceDirChangeSlices = tsdcSlices;
         accelSlices = accSlices;
         timeSinceVelChangeSlices = tsvcSlices;
         distLast20Slices = dl20Slices;
         
         stats = new double
            [vSlices.length + 1]
            [dSlices.length + 1]
            [tsdcSlices.length + 1]
            [accSlices.length + 1]
            [tsvcSlices.length + 1]
            [dl20Slices.length + 1]
            [BINS];
            
         double weight = (vSlices.length + 1)*(dSlices.length + 1)*(tsdcSlices.length +1)*(accSlices.length + 1)*(tsvcSlices.length + 1)*(dl20Slices.length + 1);
        
         if(weight < 2)
            rollingDepth = 5;
         else if(weight < 3)
            rollingDepth = 3;
         else if(weight < 7)
            rollingDepth = 1;
         else if(weight < 10)
            rollingDepth = 0.7;
         else if(weight < 33)
            rollingDepth = 0.5;
         else if(weight < 100)
            rollingDepth = 0.2;
         else rollingDepth = 0.1;
        
        // _weight = 1;
         _weight = weight;
      }
       SingleBuffer getStats(double latVel, double distance, double tsdc, double accel, double tsvc, double dl20){
         int latVelIndex = getIndex(velocitySlices, latVel);
         int distanceIndex = getIndex(distanceSlices, distance);
         int tsdcIndex = getIndex(timeSinceDirChangeSlices, tsdc);
         int accelIndex = getIndex(accelSlices,accel);
         int tsvcIndex = getIndex(timeSinceVelChangeSlices, tsvc);
         int dl20Index = getIndex(distLast20Slices, dl20);
         
         SingleBuffer sb = new SingleBuffer();
         sb.bins = stats[latVelIndex][distanceIndex][tsdcIndex][accelIndex][tsvcIndex][dl20Index];
         sb.weight = this._weight;
         sb.rollingDepth = this.rollingDepth;
         return sb;
      
      }
       private static int getIndex(double[] slices, double value){
         int index = 0;
         while(index < slices.length &&  value >= slices[index])
            index++;
         return index;
      }
   }
	
    class WaylanderGun
   {
   
      final static double angleScale = 24;
      final static double velocityScale = 1;
      static double lastEnemyHeading;
     
      static boolean firstScan;
      static StringBuilder data = new StringBuilder();
      AdvancedRobot bot;
     
     //DEBUG 
     Vector points = new Vector();
       public WaylanderGun(AdvancedRobot bot){
         this.bot = bot;
         firstScan = true;
         try{
            data.delete(60000, 80000);
         }
             catch(StringIndexOutOfBoundsException e){}
      }
   	
   
   /**
    * onScannedRobot: What to do when you see another robot
    */
       public void onScannedRobot(ScannedRobotEvent e) {
      
         double headingRadians;
         double eDistance ;
         double eHeadingRadians = e.getHeadingRadians();
         double absbearing=e.getBearingRadians()+ (headingRadians = bot.getHeadingRadians());
         
         boolean rammer = (eDistance = e.getDistance()) < 100 
            || bot.getTime() < 20;
      // 	|| Math.cos(absbearing - eHeadingRadians)*e.getVelocity() < -5 ;
       
         
         Rectangle2D.Double field = new Rectangle2D.Double(17,17,766,566);
            
      		
         if(!firstScan)
            data.insert(0,(char)((eHeadingRadians - lastEnemyHeading )*angleScale))
               .insert(0,(char)(e.getVelocity()*velocityScale));
         
        
         int keyLength  = Math.min(data.length(), Math.min(Math.max(2,(int)bot.getTime()*2 - 8), 256));
         
         int index = -1;
         do{
         
            index = data.indexOf(data.substring(0, keyLength),(int)eDistance/11)
               /2;//sorts out even/odd numbers
            
         }while(index <= 0 && (keyLength/=2) > 1);
         
         
         double bulletPower = rammer?3:Math.min(2,Math.min(bot.getEnergy()/16, e.getEnergy()/2));
         
           
         double eX=eDistance*Math.sin(absbearing);
         double eY=eDistance*Math.cos(absbearing);
         
         double db=0;
         double ww=eHeadingRadians; 
         double speed = e.getVelocity();
         double w = eHeadingRadians - lastEnemyHeading;
         do
         {
            // db+=(20-3*bulletPower); 
            if( index > 1 ){
               speed = (short)data.charAt(index*2 );
               w = ((short)data.charAt(index--*2 - 1))/angleScale;    
            }
            // eX+= (speed*Math.sin(ww));
            // eY+= (speed*Math.cos(ww));
         }while ((db+=(20-3*bulletPower))< Point2D.distance(0,0,eX+= (speed*Math.sin(ww+=w)),eY+= (speed*Math.cos(ww))) 
         && field.contains(eX + bot.getX() , eY + bot.getY()));         
         
         //DEBUG
         if(bot.getGunHeat() <= 0.1)
         points.add(new Point2D.Double(eX + bot.getX(), eY + bot.getY()));
         
         
         bot.setTurnGunRightRadians(Utils.normalRelativeAngle(Math.atan2(eX,eY) - bot.getGunHeadingRadians()));
         bot.setFire(bulletPower);
         
      
         
         bot.setTurnRadarRightRadians(Math.sin(absbearing - bot.getRadarHeadingRadians())*2);
               
         lastEnemyHeading=eHeadingRadians;
         firstScan = false;
      
      }
           
       //DEBUG ONLY

       public void onPaint(java.awt.Graphics2D g) {
         g.setColor(Color.red);
         int firstPoint = points.size() - 200;
         for(int i = 0; i < firstPoint; i++)
            points.remove(i);
         
         
         for(int i = 0; i < points.size(); i++)
            g.drawOval((int)(((Point2D.Double)(points.get(i))).x),(int)(((Point2D.Double)(points.get(i))).y),
               2,2);
      }

   
   }

	
	
    class RaikoGun {
   
   //	private static final double BEST_DISTANCE = 525;
   //	private static boolean flat = true;
      private static double bearingDirection = 1, lastLatVel, lastVelocity, /*lastReverseTime, circleDir = 1, enemyFirePower,*/ enemyEnergy, enemyDistance, lastVChangeTime, enemyLatVel, enemyVelocity/*, enemyFireTime, numBadHits*/;
      private static Point2D.Double enemyLocation;
      private static final int GF_ZERO = 15;
      private static final int GF_ONE = 30;
      private static String enemyName;
      private static int[][][][][][] guessFactors = new int[3][5][3][3][8][GF_ONE+1]; 
   //	private static double numWins;
   
      private AdvancedRobot bot;
   
       public RaikoGun(AdvancedRobot bot) {
         this.bot = bot;
      }
   
       public void run() {
      //        setColors(Color.red, Color.white, Color.white);
         // bot.setAdjustGunForRobotTurn(true);
         // bot.setAdjustRadarForGunTurn(true);
      }
   
       public void onScannedRobot(ScannedRobotEvent e) {
      
      
      /*-------- setup data -----*/
         if (enemyName == null){
         
            enemyName = e.getName();	
         //			restoreData();		
         }
         Point2D.Double robotLocation = new Point2D.Double(bot.getX(), bot.getY());
         double theta;
         double enemyAbsoluteBearing = bot.getHeadingRadians() + e.getBearingRadians();
         enemyDistance = e.getDistance();
         enemyLocation = projectMotion(robotLocation, enemyAbsoluteBearing, enemyDistance);
      
      //        if ((enemyEnergy -= e.getEnergy()) >= 0.1 && enemyEnergy <= 3.0) {
      //            enemyFirePower = enemyEnergy;
      //			enemyFireTime = bot.getTime();
      //		}
      
         enemyEnergy = e.getEnergy();
      
         Rectangle2D.Double BF = new Rectangle2D.Double(18, 18, 764, 564);
      
      //		/* ---- Movement ---- */
      //		
      //		Point2D.Double newDestination;
      //		
      //		double distDelta = 0.02 + Math.PI/2 + (enemyDistance > BEST_DISTANCE  ? -.1 : .5);
      //		
      //		while (!BF.contains(newDestination = projectMotion(robotLocation, enemyAbsoluteBearing + circleDir*(distDelta-=0.02), 170)));
      //
      //		theta = 0.5952*(20D - 3D*enemyFirePower)/enemyDistance;
      //		if ( (flat && Math.random() > Math.pow(theta, theta)) || distDelta < Math.PI/5 || (distDelta < Math.PI/3.5 && enemyDistance < 400) ){
      //			circleDir = -circleDir;
      //			lastReverseTime = getTime();
      //		}
      //		
      //		theta = absoluteBearing(robotLocation, newDestination) - getHeadingRadians();
      //		setAhead(Math.cos(theta)*100);
      //		setTurnRightRadians(Math.tan(theta));
      //		
      
      /* ------------- Fire control ------- */
      
      /*
      	To explain the below; if the enemy's absolute acceleration is
      	zero then we segment on time since last velocity change, lateral 
      	acceleration and lateral velocity.
      	If their absolute acceleration is non zero then we segment on absolute
      	acceleration and absolute velocity.
      	Regardless we segment on walls (near/far approach to walls) and distance.
      	I'm trying to have my cake and eat it, basically. :-)		
      */
         MicroWave w = new MicroWave();
      
         lastLatVel = enemyLatVel;
         lastVelocity = enemyVelocity;
         enemyLatVel = (enemyVelocity = e.getVelocity())*Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing);
      
         int distanceIndex = (int)enemyDistance/140;
      
         double bulletPower = distanceIndex == 0 ? 3 : 2;
         theta = Math.min(bot.getEnergy()/4, Math.min(enemyEnergy/4, bulletPower));
         if (theta == bulletPower)
            bot.addCustomEvent(w);
         bulletPower = theta;
         w.bulletVelocity = 20D - 3D*bulletPower;
      
         int accelIndex = (int)Math.round(Math.abs(enemyLatVel) - Math.abs(lastLatVel));
      
         if (enemyLatVel != 0)
            bearingDirection = enemyLatVel > 0 ? 1 : -1;
         w.bearingDirection = bearingDirection*Math.asin(8D/w.bulletVelocity)/GF_ZERO;
      
         double moveTime = w.bulletVelocity*lastVChangeTime++/enemyDistance;
         int bestGF = moveTime < .1 ? 1 : moveTime < .3 ? 2 : moveTime < 1 ? 3 : 4;
      
         int vIndex = (int)Math.abs(enemyLatVel/3);
      
         if (Math.abs(Math.abs(enemyVelocity) - Math.abs(lastVelocity)) > .6){
            lastVChangeTime = 0;
            bestGF = 0;
         
            accelIndex = (int)Math.round(Math.abs(enemyVelocity) - Math.abs(lastVelocity));
            vIndex = (int)Math.abs(enemyVelocity/3);
         }
      
         if (accelIndex != 0)
            accelIndex = accelIndex > 0 ? 1 : 2;
      	
         w.firePosition = robotLocation;
         w.enemyAbsBearing = enemyAbsoluteBearing;
      //now using PEZ' near-wall segment
         w.waveGuessFactors = guessFactors[accelIndex][bestGF][vIndex][BF.contains(projectMotion(robotLocation, enemyAbsoluteBearing + w.bearingDirection*GF_ZERO, enemyDistance)) ? 0 : BF.contains(projectMotion(robotLocation, enemyAbsoluteBearing + .5*w.bearingDirection*GF_ZERO, enemyDistance)) ? 1 : 2][distanceIndex];
      
       
      
         bestGF = GF_ZERO;
      
         for (int gf = GF_ONE; gf >= 0 && enemyEnergy > 0; gf--) 
            if (w.waveGuessFactors[gf] > w.waveGuessFactors[bestGF])
               bestGF = gf;
      	
         bot.setTurnGunRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - bot.getGunHeadingRadians() + w.bearingDirection*(bestGF-GF_ZERO) ));
      
      
         if (bot.getEnergy() > 1 || distanceIndex == 0)
            bot.setFire(bulletPower);
      
         bot.setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - bot.getRadarHeadingRadians()) * 2);
      
      }
   //    public void onHitByBullet(HitByBulletEvent e) {
   //		/* 
   //		The infamous Axe-hack
   //	 	see: http://robowiki.net/?Musashi 
   //		*/
   //		if ((double)(bot.getTime() - lastReverseTime) > enemyDistance/e.getVelocity() && enemyDistance > 200 && !flat) 
   //	    	flat = (++numBadHits/(bot.getRoundNum()+1) > 1.1);
   //    }
   
   
       private static Point2D.Double projectMotion(Point2D.Double loc, double heading, double distance){
      
         return new Point2D.Double(loc.x + distance*Math.sin(heading), loc.y + distance*Math.cos(heading));			
      }
   
       private static double absoluteBearing(Point2D.Double source, Point2D.Double target) {
         return Math.atan2(target.x - source.x, target.y - source.y);
      }
   
   
   //	public void onWin(WinEvent e){
   //		numWins++;
   //		saveData();
   //	}
   //	public void onDeath(DeathEvent e){
   //		saveData();	
   //	}
   
   //	//Stole Kawigi's smaller save/load methods
   //	private void restoreData(){
   //		try
   //		{
   //			ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new FileInputStream(bot.getDataFile(enemyName))));
   //			guessFactors = (int[][][][][][])in.readObject();
   //			in.close();
   //		} catch (Exception ex){flat = false;}
   //	}
   
   //	private void saveData()
   //	{
   //		if (flat && numWins/(getRoundNum()+1) < .7 && getNumRounds() == getRoundNum()+1)
   //		try{
   //			ObjectOutputStream out = new ObjectOutputStream(new GZIPOutputStream(new RobocodeFileOutputStream(getDataFile(enemyName))));
   //			out.writeObject(guessFactors);
   //			out.close();
   //		}
   //		catch (IOException ex){}
   //	}
   
   
       class MicroWave extends Condition
      {
      
         Point2D.Double firePosition;
         int[] waveGuessFactors;
         double enemyAbsBearing, distance, bearingDirection, bulletVelocity;
      
          public boolean test(){
         
            if ((RaikoGun.enemyLocation).distance(firePosition) <= (distance+=bulletVelocity) + bulletVelocity){
               try {
                  waveGuessFactors[(int)Math.round((Utils.normalRelativeAngle(absoluteBearing(firePosition, RaikoGun.enemyLocation) - enemyAbsBearing))/bearingDirection + GF_ZERO)]++;
               } 
                   catch (ArrayIndexOutOfBoundsException e){}
               bot.removeCustomEvent(this);
            }
            return false;
         }
      }
   
   
   }   		       