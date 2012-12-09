using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using Robocode;

namespace Mcg.Robocode.Diagnostics
{
    public interface IDiagnosticPropertyHandler
    {
        string this[string name]
        {
            set;
        }
    }

    public static class Debug
    {
        private static TextWriter s_output;
        private static IDiagnosticPropertyHandler s_propertyHandler;

        static Debug()
        {
            SetNullOutput();
        }

        public static void SetOuput(TextWriter output)
        {
            s_output = output;
        }

        public static void SetPropertyHandler(IDiagnosticPropertyHandler handler)
        {
            s_propertyHandler = handler;
        }

        public static void SetNullOutput()
        {
            s_output = new StreamWriter(Stream.Null);
            s_propertyHandler = new NullPropertyHandler();
        }

        public static TextWriter Out
        {
            get { return s_output; }
        }

        public static IDiagnosticPropertyHandler Property
        {
            get { return s_propertyHandler; }
        }

        public static void Assert(bool condition, string format, params string[] args)
        {
            if (!condition)
            {
                string msg = String.Format(format, args);
                Out.WriteLine("Assertion Failed: {0}", msg);
            }
        }
    }

    public class NullPropertyHandler : IDiagnosticPropertyHandler
    {
        public string this[string name]
        {
            set { }
        }
    }

    public class RobotPropertyHandler : IDiagnosticPropertyHandler
    {
        private Robot.DebugPropertyH m_botProp;

        public RobotPropertyHandler(Robot.DebugPropertyH prop)
        {
            m_botProp = prop;
        }

        public string this[string name]
        {
            set { m_botProp[name] = value; }
        }
    }

    public class DictionaryPropertyHandler : IDiagnosticPropertyHandler
    {
        private Dictionary<string, string> m_dictionary = new Dictionary<string,string>();
    
        public string this[string name]
        {
            set { m_dictionary[name] = value; }
        }
    }
}
