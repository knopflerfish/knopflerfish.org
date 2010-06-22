The readme file is obsolete, it has been replaced by an entry in the
"Bundle user doc" available at:

http://www.knopflerfish.org/releases/current/docs/bundledoc/index.html?docpage=console


============================ Old Obsolete Contents ============================

   Console User's Guide
                                  Abstract

   The user's guide contains information about the console interfaces,
   the command group concept and reference documentation for the
   console's built in commands.

1 Terminology

   Terms, acronyms and syntax of console commands.

  1.1 Terms and Acronyms

   command group
          A set of commands exported to the console from some bundle.

   EOF
          End Of File, close of console input.

   session
          What you are in when communicating with the console

   session group
          The command group for the console built in commands.

  1.2 Syntax Elements

   []
          Delimiters for optional parts of a command and optional
          parameters.

   |
          To indicate alternate parameters.

   ...
          More parameters, same type as the previous one.

   #value#
          Parameter to a flag.

   " "
          Double quotes. Used to contain parameters that have blanks in
          them.

   <parameter>
          Command parameter to be replaced with data.

2 Console Interfaces

   The console bundle is not very useful on its own; it needs other
   bundles to supply the commands and also at least one bundle supplying
   a user interface. The console uses the CommandGroup service of the
   command supplying bundles to access commands. A console user interface
   bundle uses the ConsoleService service of the console bundle to
   execute commands ordered by the user.

   The console bundle actually has some built in commands that can be
   used for administrative tasks, that is, managing the user session.
   These are the commands in the command group session.

   The following bundles implement user interfaces:

   consoletty
          Basic tty console, available in the terminal window where the
          platform is started.

   consoletelnet
          Basic telnet console, listens on a port using the telnet protocol.

3 General Command Structure and Behavior

   The general command format is built of:
<command group> <command> [<flag [#parameter#]> ...] [<parameter> ...]

   First is the command group (all commands must belong to a command
   group), then the command. After the command is zero, one or more flags
   with possible additional parameters and finally zero, one or more
   parameters. See chapter [2]Terminology on page 1 for explanations of
   delimiters.

   Note: To avoid having negative numbers interpreted as flags, negative
   numbers are to be written with double hyphens (as --n).

   At start-up, the console is in the initial state. When entering a
   command the command line must begin with the command group's name, as
   described above. However, the session can enter a group with the
   session enter command. After entering a command group, commands of
   that group are executed without the group name as prefix. The session
   leave command leaves the current group and the session is back in its
   initial state.

   Commands from other groups than the current group can be executed by
   prefixing the command with a slash ("/"). For example, the following
   would execute the shutdown command from the framework command group,
   regardless of the session's current group.
  /framework shutdown

   Normally, command groups and commands may be shortened as long as they
   are unambiguously identifiable. As an example, in command group
   session, the command alias may be shortened to a as it is the only
   command that starts with the letter a.

4 Session Commands

  4.1 Overview

   This is the command group for the console's built in administrative
   commands. It contains commands for managing a session.

   All of the session commands have aliases to make them quick to enter
   regardless of the current command group. For example, /session help
   has the alias help. In all the examples below, the alias versions of
   the session commands are used.

   The commands in the session command group are:
     * alias [<alias>] [<val>] ...
     * enter <command group>
     * help
     * leave
     * prompt <prompt>
     * quit
     * save
     * unalias <alias name>

  4.2 Command Details

   Detailed description of all session commands in alphabetical order.

    4.2.1 alias

   To set or show aliases.
  alias [<alias>] [<value>] ...

   The command without any parameters prints a list of all existing
   aliases:
  > alias
  start = /framework start
  install = /framework install
  prompt = /session prompt
  lsb = /framework bundles
  fw = /session enter framework
  log = /log show
  help = /session help
  quit = /session quit
  lss = /framework services
  alias = /session alias
  unalias = /session unalias
  enter = /session enter
  stop = /framework stop
  bundles = /framework bundles
  leave = /session leave
  >

   With parameters it sets an alias to the specified value. If the alias
   exists, the old value is replaced with the new. With one parameter the
   value of that alias is shown.

    4.2.2 enter

   Enter a command group, in effect automatically prefix all commands
   with the name of the command group. This makes it possible to use the
   short names of the group's commands.
  enter <command group>

   The result is that the prompter is prefixed with the command group and
   all the commands in the command group are available in short form.
   Example: Entering a command group
  > enter framework
  framework> help
  Available framework commands:
    bundles [-help] [-1] [-i] [-l] [<bundle>] ... - List bundles
    call [-help] <interface> <method> [<args>] ...
      - Call a method in a registered service
    headers [-help] <bundle> ... - Show bundle header values
    package [-help] <package> ... - Show java package information
    install [-help] [-s] <location> ... - Install one or more bundles
    services [-help] [-i] [-l] [-r] [-s] [-u] [<bundle>] ...
      - List registered services
    start [-help] <bundle> ... - Start one or more bundles
    stop [-help] <bundle> ... - Stop one or more bundles
    shutdown [-help] [<exit code>] - Shutdown framework
    uninstall [-help] >bundle> ... - Uninstall one or more bundles
    update [-help] [-r] <bundle> ... - Update one or more bundles

   It is only possible to be in one command group at a given moment. By
   adding a slash ("/") to the group name, commands in other gruops can
   be accessed.

    4.2.3 help

   Show help information about commands and command groups.
  help [<command group> | all]

   Lists the commands available in the specified command group, each with
   a short description. If no command group is specified, help for the
   current group is displayed.

   In the initial state, or if the parameter all is supplied, help shows
   the available command groups.
   Example: Display available command groups
  > help
  Available command groups:
  session - Session commands built into the console
  logconfig - Configuration commands for the log.
  log - Log commands
  framework - Framework commands
  >

   Note that this list can be longer as any installed bundle can export
   its own commands.

    4.2.4 leave

   Leave a command group, that is, go back to the initial state (no
   current command group).
  leave

   Example: Leave the current command group
  framework> leave
  >

   Note that leave only goes to the initial state, it does not go to the
   previous command group, if any.

    4.2.5 prompt

   Set the command prompter.
  prompt <command prompt>

   If the command group is to be visible in the prompt, a percent
   character ("%") should be included in the prompt string. At printout,
   the % character will be replaced by the command group name.
   Example: Changing the prompter
  > prompt "%test >"
  test >
  enter framework
  frameworktest >

    4.2.6 quit

   Exit the session.
  quit

   The console exits and loses contact with standard in.

    4.2.7 save

   This command saves the current aliases to persistent memory. The
   aliases are read next time the platform is started.
  save

    4.2.8 unalias

   This command removes an alias.
  unalias

   Example: Creating and removing an alias
  > alias more less
  > unalias more

5 Interface consoletty

   The consoletty bundle allows local console access to the platform
   without the use of the http server.

   If the platform starts the consoletty, it will use the text window the
   platform was started from.

6 Interface consoletcp

   To allow remote console access to the platform without the use of the
   http server, the consoletcp bundle listens to a port on the platform.
   See [[3]1] information regarding the port number.

   Depending on configuration, the TCP console will require that the user
   logs in before creating a console session. See [[4]1] for more
   information.

   The user name/password authentication method is used for
   authentication. The Input Path is set to "tcp" and the Auhtentication
   Method is set to "passwd".

   The commands and their formats are the same as for the consoletty. To
   access it in a simple way, use telnet.
   Example: Using telnet and consoletcp
> telnet demo.gatespace.se 8999
Trying 127.0.0.1...
Connected to localhost.localdomain.
Escape character is '^]'.
> help
Available command groups:
session - Session commands built into the console
osgilog - Log commands
messenger - Messenger route configuration commands.
logconfig - Configuration commands for the log.
framework - Framework commands

>

7 Environmental influences

   Due to the fact that console windows may be opened in many different
   environments that have different capabilities, the console may seem to
   behave different.

   One example is the occurence of a "command history" in some
   environments. This capability is however supplied by the window
   manager "locally", the console does not have any command history
   capability.

