NOTHING EXPLORER // COMPREHENSIVE USER MANUAL
Nothing Explorer is an industrial-grade file manager and network storage station designed with the distinctive Nothing OS (NDot) aesthetic. It combines local device file navigation, in-app document viewing and editing, archive handling, remote storage protocol integrations, and a dedicated VPN / Homelab LAN bridge.
1. Core Design & Aesthetics (Nothing OS / Glyph Identity)
NDot Typography & Monospaced Metadata: File details, sizes, paths, and status telemetry are presented in monospaced and dot-matrix inspired fonts.
High-Contrast Monochrome Theme: Crisp black, dark gray, and white surfaces accented with signature Nothing Red and Nothing Amber/Green status dots.
Adaptive Light & Dark Modes: Dynamic contrast optimization for input fields, text labels, and borders across all lighting environments.
Interactive Glyphs & Quick Status: Header bars and storage badges show real-time storage metrics, free capacity, and active connection indicators.
2. File & Directory Management
Dual-Pane Navigation & Breadcrumbs: Tap any segment in the breadcrumb trail to jump backwards; navigate forward through directories with smooth transitions.
Full File Operations:
Create: Add new text files (.txt, .md, .log) or new folders.
Rename: Fast inline or dialog renaming for any file or directory.
Copy, Cut & Paste: Move or duplicate files across local folders, SD storage, or mounted network drives.
Delete: Safe deletion confirmation with item count and size details.
Batch Selection: Multi-select files for bulk copy, move, compress, or delete operations.
Sort & Filter:
Sort by Name (A–Z / Z–A), Date Modified, Size, or Type.
Real-Time Search Bar: Instant incremental search filtering as you type.
Quick Access Categories:
Instant filters for Downloads, Documents, Images, Audio, Videos, and Archives.
3. Storage Cleanup & Space Analyzer
Interactive Storage Ring/Bar: Visual representation of internal storage usage (Used vs. Free vs. System).
Categorized Space Breakdown: Displays storage consumed by Media, Documents, Downloads, and System caches.
Large File Hunter: Quickly surfaces files exceeding 50 MB / 100 MB for fast space reclamation.
Duplicate & Temporary File Finder: Identifies temporary logs and redundant downloads.
4. In-App Document Viewers & Editors
You can view, search, and edit files directly within Nothing Explorer without third-party apps:
A. PDF Viewer
Multi-page rendering with smooth scrolling.
Keyword search with highlight navigation.
Page jump bar and thumbnail navigation.
Zoom controls (fit-to-width, pinch zoom).
B. Excel & Spreadsheet Viewer (.xlsx, .xls, .csv)
Multi-sheet tab selector for multi-tab workbooks.
Interactive cell grid with row/column headers.
Cell value and formula bar editing (supports formulas such as =SUM(...)).
Column search and data filtering.
C. Word & Document Viewer (.docx)
Structured layout rendering (headings, paragraphs, bullet lists, tables).
Text search inside document content.
Document outline / section navigation.
D. Text & Code Editor (.txt, .md, .json, .py, .xml, .log, etc.)
Line numbers and monospaced programming typography.
Line filtering / search within files.
Direct inline editing, undo/redo, and direct file save.
E. Media Players
Image Viewer: High-resolution zoom, pan, and EXIF metadata inspector.
Audio & Video Previews: Integrated lightweight media playback for quick verification.
5. Archive & Compression Manager
Create ZIP Archives: Select one or multiple files/folders and package them into standard .zip archives.
Extract Archives: Extract .zip (and view contents of .tar / .gz) directly into custom destination folders.
In-Archive Previews: Inspect the contents and file listing of an archive before extracting.
6. Remote Network & Cloud Storage Mounts
Mount remote shares directly into the file tree alongside local storage:
SMB / Windows Share / Samba:
Connect to NAS drives, Windows shared folders, TrueNAS, Unraid, or Samba servers.
Supports custom ports (default 445) and optional guest/user credentials.
Automatic IP validation and subnet matching for local homelab environments.
WebDAV:
Connect to Nextcloud, ownCloud, Synology WebDAV, or Apache WebDAV servers (http:// and https://).
Cloud Storage Integrations:
Pre-configured profiles and token sign-in dialogs for Google Drive, Microsoft OneDrive, and Dropbox.
Unified File Access:
Browse, upload, download, and stream files from remote shares directly through the file list.
7. Integrated VPN & Homelab LAN Bridge
Designed for remote access to private servers, home labs, and firewalls:
Routing Scopes:
Direct LAN / Homelab Bridge: Zero TUN packet capture mode; connects directly to your local subnet gateways (192.168.4.1/22, 10.10.0.1/22, 192.168.0.1/22) without interfering with general web browsing.
Drive Only (Subnets): Per-app split tunnel routing only file transfers, SMB shares, and configured subnets through the VPN.
Whole Phone (Subnet-Safe): Routes your homelab subnets without blackholing internet and external websites.
Subnet Engine (/22 CIDR Arithmetic):
Built-in SubnetUtil calculates netmasks, broadcast addresses, and usable host ranges.
Automatically zeros host bits when supplying routes to Android's VpnService.Builder.addRoute to prevent routing exceptions.
Live subnet auto-detection when entering host IPs in Mount Drive dialogs.
OpenVPN & Sophos Firewall Profiles:
Import .ovpn configuration profiles.
Configure UDP / TCP protocols, custom cipher options (AES-256-GCM, etc.), MTU, DNS servers, and credentials.
Live Connection Diagnostics & Metrics:
Real-time speedometer for upload and download rates.
Cumulative bytes in / bytes out tracking.
Gateway latency prober: measures response times (in milliseconds) across configured homelab subnets and ports (445, 80, 443, 22).
Terminal-style live connection log with real-time log search and filtering.
8. Summary of Dialogs & Quick Controls
Component	Shortcut / Trigger	Description
Mount Drive	Top Bar / FAB	Add SMB, WebDAV, or Cloud storage providers
VPN Station	Shield / Router Icon	Manage homelab VPN bridges, profiles, subnets, and logs
New Folder / File	Floating Action Menu	Create new folders or empty text files
Search Filter	Magnifying Glass	Instant text filter for the active directory
Space Cleaner	Storage Card	Scan for large files, duplicates, and system cache
Archive Tool	Context Menu on file(s)	Compress into .zip or extract existing archives
