Jar unpacker code. Used by distribution to create a self-extracting
executable jar file.


-----------------------------------
Manifest attributes:

Main-class should
   org.knopflerfish.tools.jarunpacker.Main"/>

Default unpack destination directory
  jarunpacker-destdir


Directory which should be aut-opened after install
  jarunpacker-opendir

 $(destdir) will be replaced by user-selected destination dir


Version string for distrib
  knopflerfish-version


Path to image resource for icon logo.
If unset, use default icon
  jarunpacker-iconpath

Placement of icon logo.
If true place icon on left side, otherwise at the top.
  jarunpacker-iconleft

Path to resource with licenset text.
If unset, use default license -->
  jarunpacker-licensepath

Header text for page with license
 jarunpacker-licensetitle

Window frame title. If unset use default title -->
 jarunpacker-windowtitle
