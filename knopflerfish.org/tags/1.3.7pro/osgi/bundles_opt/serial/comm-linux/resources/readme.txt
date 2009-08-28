This bundle contains binaries from the RXTX library, distributed
under LGPL, see

 http://users.frii.com/jarvi/rxtx/

and the included license file

 rxtx-license.txt
 

Troubleshooting:

1) NoSuchPortException:
If you happen to encounter a NoSuchPortException, which should
from your point of view not occur, you may want to check the
device-nodes permission.
It is most likely, that the device-node you are going to use
does not allow neither reading nor writing.

Please adjust these permissions to make the user executing the
framework is able to read and write to the correspindig node.
Consult you device-node (e.g. devfs, udev)  managers documentation
for the necessary settings to make these adjustments permanent.


2) check_group_uucp(): error testing lock file creation Error details
If you encounter this message on starting the bundle you don't have
permissions to write in /var/lock.
This is necessary to avoid race conditions, when there are multiple
accessors waiting for a port .
Make sure, your user is in a group, which is able to write in /var/lock.
This is mostly the case for group uucp.
Check the INSTALL documentation, which comes with RXTX' package for 
further details.
