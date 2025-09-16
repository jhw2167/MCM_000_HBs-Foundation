# HB's Foundation

Library mod for any of Holy Buckets mods. Contains utility functions and commonly reused event, registry, and networking objects.
Also allows me to store worldGen data indepent of chunks or blockEntities in hb_datastore.json file the world folder.


v1.3.0
	- Explicit support for client and server exclusive events
	- Updated DataStore to save itself inside a particular world folder
	- Added support for PlayerWakeUp event and improved support for daily tick events
	- Added internal support for event priority for critical server and level loading events since fabric does not include it

v1.2.2
	- Fixed a bug where mod would crash on servers
	- Upgraded to Balm 7.34