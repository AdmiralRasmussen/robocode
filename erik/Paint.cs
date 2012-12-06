using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Text;
using Robocode;

namespace goro
{
    internal static class Paint
    {
        private const int BotSize = 28;

        public static void BotCircle(IGraphics graphics, double X, double Y, bool fill)
        {
            if (fill)
            {
                var transparentGreen = new SolidBrush(Color.FromArgb(30, 0, 0xFF, 0));
                graphics.FillEllipse(transparentGreen, (int)(X - 60), (int)(Y - 60), 120, 120);
            }
            graphics.DrawEllipse(Pens.Red, (int)(X - 50), (int)(Y - 50), 100, 100);
        }

        public static void BotDot(IGraphics graphics, double X, double Y)
        {
            var opaqueYellow = new SolidBrush(Color.FromArgb(0xFF, 0xFF, 0xFF, 0));
            graphics.FillEllipse(opaqueYellow, (int)(X - 2), (int)(Y - 2), 4, 4);
        }

        public static void BotBox(IGraphics graphics, Pen pen, double X, double Y, double Heading)
        {
            var Corner1 = Geometry.MakePoint(X,Y).VectorProjection(Heading + 45, BotSize);
            var Corner2 = Geometry.MakePoint(X,Y).VectorProjection(Heading + 135, BotSize);
            var Corner3 = Geometry.MakePoint(X,Y).VectorProjection(Heading + 225, BotSize);
            var Corner4 = Geometry.MakePoint(X,Y).VectorProjection(Heading + 315, BotSize);
            graphics.DrawLine(pen, Corner1, Corner2);
            graphics.DrawLine(pen, Corner2, Corner3);
            graphics.DrawLine(pen, Corner3, Corner4);
            graphics.DrawLine(pen, Corner4, Corner1);
        }

        public static void BotForecast(IGraphics graphics, double X, double Y, double Heading, double Velocity, int Turns)
        {
            BotPosition bot = new BotPosition(X, Y, Heading, Velocity);
            BotPosition[][] moves = BotPosition.AllMoves(bot, Turns);

            foreach (BotPosition[] path in moves)
            {
                int i = 0;
                //float LastX = (float)X;
                //float LastY = (float)Y;
                foreach (BotPosition b in path)
                {
                    if (i++ % 4 == Turns % 4)
                    //if (i++ % 5 == Turns % 5 && Turns/i < 2)
                    {
                        BotDot(graphics, b.Location.X, b.Location.Y);
                        //BotBox(graphics, new Pen(Color.FromArgb(100, 255, 255, 0)), b.Location.X, b.Location.Y, b.Heading);
                        //graphics.DrawRectangle(new Pen(Color.FromArgb(100, 255, 255, 0)), b.Location.X - 18, b.Location.Y - 18, 36, 36);
                        //graphics.DrawLine(new Pen(Color.FromArgb(100, 255, 255, 0)), LastX, LastY, (float)b.Location.X, (float)b.Location.Y);
                        //LastX = (float)b.Location.X;
                        //LastY = (float)b.Location.Y;
                    }
                }
            }
        }

        public static void BotVelocityVector(IGraphics graphics, double X, double Y, double HeadingRadians, double Velocity)
        {
            double BotVx = X + Math.Cos(Math.PI / 2 - HeadingRadians) * Velocity * 10;
            double BotVy = Y + Math.Sin(Math.PI / 2 - HeadingRadians) * Velocity * 10;
            graphics.DrawLine(Pens.Yellow, (float)X, (float)Y, (float)BotVx, (float)BotVy);
        }
    }
}
