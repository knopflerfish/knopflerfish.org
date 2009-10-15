/*
 * Oscar Bundle Repository
 * Copyright (c) 2004, Richard S. Hall
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *   * Neither the name of the ungoverned.org nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Contact: Richard S. Hall (heavy@ungoverned.org)
 * Contributor(s):
 *
**/
package org.ungoverned.osgi.bundle.bundlerepository;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import kxml.sax.KXmlSAXParser;

import org.osgi.framework.*;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.ungoverned.osgi.service.bundlerepository.*;

import fr.imag.adele.metadataparser.MultivalueMap;
import fr.imag.adele.metadataparser.XmlCommonHandler;

public class BundleRepositoryServiceImpl implements BundleRepositoryService
{
    private BundleContext m_context = null;
    private boolean m_initialized = false;
    private String[] m_urls = null;
    private List m_bundleList = new ArrayList();
    private Map m_exportPackageMap = new HashMap();

    private static final int EXPORT_PACKAGE_IDX = 0;
    private static final int EXPORT_BUNDLE_IDX = 1;

    private int m_hopCount = 1;

    private static final String[] DEFAULT_REPOSITORY_URL = {
        "http://oscar-osgi.sf.net/repository.xml"
    };

    public static final String REPOSITORY_URL_PROP
        = "oscar.repository.url";

    public static final String EXTERN_REPOSITORY_TAG = "extern-repositories";

    // These repositories are no more. Silently ignore them.
    private String[] noMoreHosts = new String[]{
        "domoware.isti.cnr.it",
        "update.cainenable.org",
    };


    public BundleRepositoryServiceImpl(BundleContext context)
    {
        m_context = context;
        String urlStr = context.getProperty(REPOSITORY_URL_PROP);
        if (urlStr != null)
        {
            StringTokenizer st = new StringTokenizer(urlStr);
            if (st.countTokens() > 0)
            {
                m_urls = new String[st.countTokens()];
                for (int i = 0; i < m_urls.length; i++)
                {
                    m_urls[i] = st.nextToken();
                }
            }
        }

        // Use the default URL if none were specified.
        if (m_urls == null)
        {
            m_urls = DEFAULT_REPOSITORY_URL;
        }
    }

    public String[] getRepositoryURLs()
    {
        if (m_urls != null)
        {
            // Return a copy because the array is mutable.
            return (String[]) m_urls.clone();
        }
        return null;
    }

    public synchronized void setRepositoryURLs(String[] urls)
    {
        if (urls != null)
        {
            m_urls = urls;
            initialize();
        }
    }

    /**
     * Get the number of bundles available in the repository.
     * @return the number of available bundles.
    **/
    public synchronized int getBundleRecordCount()
    {
        if (!m_initialized)
        {
            initialize();
        }

        return m_bundleList.size();
    }

    /**
     * Get the specified bundle record from the repository.
     * @param i the bundle record index to retrieve.
     * @return the associated bundle record or <tt>null</tt>.
    **/
    public synchronized BundleRecord getBundleRecord(int i)
    {
        if (!m_initialized)
        {
            initialize();
        }

        if ((i < 0) || (i >= getBundleRecordCount()))
        {
            return null;
        }

        return (BundleRecord) m_bundleList.get(i);
    }

    /**
     * Get bundle record for the bundle with the specified name
     * and version from the repository.
     * @param name the bundle record name to retrieve.
     * @param version three-interger array of the version associated with
     *        the name to retrieve.
     * @return the associated bundle record or <tt>null</tt>.
    **/
    public synchronized BundleRecord getBundleRecord(String name, int[] version)
    {
        if (!m_initialized)
        {
            initialize();
        }

        BundleRecord[] records = getBundleRecords(name);

        if (records.length > 0)
        {
            for (int i = 0; i < records.length; i++)
            {
                String targetName =
                    (String) records[i].getAttribute(BundleRecord.BUNDLE_NAME);
                int[] targetVersion = Util.parseVersionString(
                    (String) records[i].getAttribute(BundleRecord.BUNDLE_VERSION));

                if ((targetName != null) &&
                    targetName.equalsIgnoreCase(name) &&
                    (Util.compareVersion(targetVersion, version) == 0))
                {
                    return records[i];
                }
            }
        }

        return null;
    }

    /**
     * Get all versions of bundle records for the specified name
     * from the repository.
     * @param name the bundle record name to retrieve.
     * @return an array of all versions of bundle records having the
     *         specified name or <tt>null</tt>.
    **/
    public synchronized BundleRecord[] getBundleRecords(String name)
    {
        if (!m_initialized)
        {
            initialize();
        }

        BundleRecord[] records = new BundleRecord[0];

        for (int i = 0; i < m_bundleList.size(); i++)
        {
            String targetName = (String)
                getBundleRecord(i).getAttribute(BundleRecord.BUNDLE_NAME);
            if ((targetName != null) && targetName.equalsIgnoreCase(name))
            {
                BundleRecord[] newRecords = new BundleRecord[records.length + 1];
                System.arraycopy(records, 0, newRecords, 0, records.length);
                newRecords[records.length] = getBundleRecord(i);
                records = newRecords;
            }
        }

        return records;
    }

    public boolean deployBundle(
        PrintStream out, PrintStream err, String updateLocation,
        boolean isResolve, boolean isStart)
    {
        // List to hole bundles that need to be started.
        List startList = null;

        // The first thing to do is to see if the bundle to be
        // deployed is locally installed. If so, then we have to
        // check if an update is available for it. If no update
        // is available then we just return. NOTE: This is sort
        // of a hack that is necessary because the resolvePackages()
        // method doesn't do this sort of checking, so we have
        // to do it here first.
        Bundle localBundle = findLocalBundleByUpdate(updateLocation);
        if (localBundle != null)
        {
            if (!isUpdateAvailable(out, err, localBundle))
            {
                out.println("No update available: "
                    + Util.getBundleName(localBundle));
                return false;
            }
        }

        // Find the appropriate bundle record in the repository,
        // then calculate the transitive closure of its dependencies.
        BundleRecord record = findBundleRecordByUpdate(updateLocation);
        if (record != null)
        {
            // This set will contain all bundles that must be
            // deployed as a result of deploying the target bundle;
            // use a list because this will keep everything in order.
            List deployList = new ArrayList();
            // Add the target bundle to the set, since it will
            // ultimately be deployed as a result of this request.
            deployList.add(updateLocation);
            // If the resolve flag is set, then get its imports to
            // calculate the transitive closure of its dependencies.
            if (isResolve)
            {
                PackageDeclaration[] imports = (PackageDeclaration[])
                    record.getAttribute(BundleRecord.IMPORT_PACKAGE);
                try
                {
                    resolvePackages(imports, deployList);
                }
                catch (ResolveException ex)
                {
                    err.println("Resolve error: " + ex.getPackageDeclaration());
                    return false;
                }
            }

            // Now iterate through all bundles in the deploy list
            // in reverse order and deploy each; the order is not
            // so important, but by reversing them at least the
            // dependencies will be printed first and perhaps it
            // will avoid some ordering issues when we are starting
            // bundles.
            for (int i = deployList.size() - 1; i >= 0; i--)
            {
                String deployLocation = (String) deployList.get(i);

                // See if the bundle is locally installed, in which case
                // we will be performing an update.
                localBundle = findLocalBundleByUpdate(deployLocation);
                if (localBundle != null)
                {
                    // Verify that the locally installed bundle has an
                    // update available for it; if not, then ignore it.
                    if (!isUpdateAvailable(out, err, localBundle))
                    {
                        continue;
                    }

                    // Print out an "updating" message.
                    if (!deployLocation.equals(updateLocation))
                    {
                        out.print("Updating dependency: ");
                    }
                    else
                    {
                        out.print("Updating: ");
                    }
                    out.println(Util.getBundleName(localBundle));

                    // Actually perform the update.
                    try
                    {
                        localBundle.update();
                    }
                    catch (BundleException ex)
                    {
                        err.println("Update error: " + Util.getBundleName(localBundle));
                        ex.printStackTrace(err);
                        return false;
                    }
                }
                else
                {
                    // Print out an "installing" message.
                    if (!deployLocation.equals(updateLocation))
                    {
                        out.print("Installing dependency: ");
                    }
                    else
                    {
                        out.print("Installing: ");
                    }
                    record = findBundleRecordByUpdate(deployLocation);
                    out.println(record.getAttribute(BundleRecord.BUNDLE_NAME));

                    // Actually perform the install.
                    try
                    {
                        Bundle bundle = m_context.installBundle(deployLocation);

                        // If necessary, save the installed bundle to be
                        // started later.
                        if (isStart)
                        {
                            if (startList == null)
                            {
                                startList = new ArrayList();
                            }
                            startList.add(bundle);
                        }
                    }
                    catch (BundleException ex)
                    {
                        err.println("Install error: "
                            + record.getAttribute(BundleRecord.BUNDLE_NAME));
                        ex.printStackTrace(err);
                        return false;
                    }
                }
            }

            // If necessary, start bundles after installing them all.
            if (isStart)
            {
                for (int i = 0; (startList != null) && (i < startList.size()); i++)
                {
                    localBundle = (Bundle) startList.get(i);
                    try
                    {
                        localBundle.start();
                    }
                    catch (BundleException ex)
                    {
                        err.println("Update error: " + Util.getBundleName(localBundle));
                        ex.printStackTrace();
                    }
                }
            }

            return true;
        }

        return false;
    }

    public BundleRecord[] resolvePackages(PackageDeclaration[] pkgs)
        throws ResolveException
    {
        // Create a list that will contain the transitive closure of
        // all import dependencies; use a list because this will keep
        // everything in order.
        List deployList = new ArrayList();
        resolvePackages(pkgs, deployList);

        // Convert list of update locations to an array of bundle
        // records and return it.
        BundleRecord[] records = new BundleRecord[deployList.size()];
        for (int i = 0; i < deployList.size(); i++)
        {
            String updateLocation = (String) deployList.get(i);
            records[i] = findBundleRecordByUpdate(updateLocation);
        }
        return records;
    }

    public void resolvePackages(
        PackageDeclaration[] pkgs, List deployList)
        throws ResolveException
    {
        for (int pkgIdx = 0;
            (pkgs != null) && (pkgIdx < pkgs.length);
            pkgIdx++)
        {
            // If the package can be locally resolved, then
            // it can be completely ignored; otherwise, try
            // to find a resolving bundle.
            if (!isLocallyResolvable(pkgs[pkgIdx]))
            {
                // Select resolving bundle for current package.
                BundleRecord source = selectResolvingBundle(pkgs[pkgIdx]);
                // If there is no resolving bundle, then throw a
                // resolve exception.
                if (source == null)
                {
                    throw new ResolveException(pkgs[pkgIdx]);
                }
                // Get the update location of the resolving bundle.
                String updateLocation = (String)
                    source.getAttribute(BundleRecord.BUNDLE_UPDATELOCATION);
                // If the resolving bundle's update location is already
                // in the deploy list, then just ignore it; otherwise,
                // add it to the deploy list and resolve its packages.
                if (!deployList.contains(updateLocation))
                {
                    deployList.add(updateLocation);
                    PackageDeclaration[] imports = (PackageDeclaration[])
                        source.getAttribute(BundleRecord.IMPORT_PACKAGE);
                    resolvePackages(imports, deployList);
                }
            }
        }
    }

    /**
     * Returns a locally installed bundle that has an update location
     * manifest attribute that matches the specified update location
     * value.
     * @param updateLocation the update location attribute for which to search.
     * @return a bundle with a matching update location attribute or
     *         <tt>null</tt> if one could not be found.
    **/
    private Bundle findLocalBundleByUpdate(String updateLocation)
    {
        Bundle[] locals = m_context.getBundles();
        for (int i = 0; i < locals.length; i++)
        {
            String localUpdateLocation = (String)
                locals[i].getHeaders().get(BundleRecord.BUNDLE_UPDATELOCATION);
            if ((localUpdateLocation != null) &&
                localUpdateLocation.equals(updateLocation))
            {
                return locals[i];
            }
        }
        return null;
    }

    private boolean isUpdateAvailable(
        PrintStream out, PrintStream err, Bundle bundle)
    {
        // Get the bundle's update location.
        String updateLocation =
            (String) bundle.getHeaders().get(Constants.BUNDLE_UPDATELOCATION);

        // Get associated repository bundle recorded for the
        // local bundle and see if an update is necessary.
        BundleRecord record = findBundleRecordByUpdate(updateLocation);
        if (record == null)
        {
            err.println(Util.getBundleName(bundle) + " not in repository.");
            return false;
        }

        // Check bundle version againts bundle record version.
        int[] bundleVersion = Util.parseVersionString(
            (String) bundle.getHeaders().get(Constants.BUNDLE_VERSION));
        int[] recordVersion = Util.parseVersionString(
            (String) record.getAttribute(BundleRecord.BUNDLE_VERSION));
        if (Util.compareVersion(recordVersion, bundleVersion) > 0)
        {
            return true;
        }
        return false;
    }

    /**
     * Returns the bundle record corresponding to the specified update
     * location string; update location strings are assumed to be
     * unique.
     * @param updateLocation the update location of the bundle record
     *        to retrieve.
     * @return the corresponding bundle record or <tt>null</tt>.
    **/
    private synchronized BundleRecord findBundleRecordByUpdate(String updateLocation)
    {
        if (!m_initialized)
        {
            initialize();
        }

        for (int i = 0; i < m_bundleList.size(); i++)
        {
            String location = (String)
                getBundleRecord(i).getAttribute(BundleRecord.BUNDLE_UPDATELOCATION);
            if ((location != null) && location.equalsIgnoreCase(updateLocation))
            {
                return getBundleRecord(i);
            }
        }
        return null;
    }

    /**
     * Determines whether a package is resolvable at the local framework.
     * @param target the package declaration to check for availability.
     * @return <tt>true</tt> if the package is available locally,
     *         <tt>false</tt> otherwise.
    **/
    private synchronized boolean isLocallyResolvable(PackageDeclaration target)
    {
        // Get package admin service.
        ServiceReference ref = m_context.getServiceReference(
            "org.osgi.service.packageadmin.PackageAdmin");
        if (ref == null)
        {
            return false;
        }

        PackageAdmin pa = (PackageAdmin) m_context.getService(ref);
        if (pa == null)
        {
            return false;
        }

        ExportedPackage[] exports = pa.getExportedPackages((Bundle)null);
        if (exports != null)
        {
            for (int i = 0;
                (exports != null) && (i < exports.length);
                i++)
            {
                PackageDeclaration source =
                    new PackageDeclaration(
                        exports[i].getName(),
                        exports[i].getSpecificationVersion());
                if (source.doesSatisfy(target))
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Selects a single source bundle record for the target package from
     * the repository. The algorithm tries to select a source bundle record
     * if it is already installed locally in the framework; this approach
     * favors updating already installed bundles rather than installing
     * new ones. If no matching bundles are installed locally, then the
     * first bundle record providing the target package is returned.
     * @param targetPkg the target package for which to select a source
     *        bundle record.
     * @return the selected bundle record or <tt>null</tt> if no sources
     *         could be found.
    **/
    private BundleRecord selectResolvingBundle(PackageDeclaration targetPkg)
    {
        BundleRecord[] sources = findResolvingBundles(targetPkg);

        if (sources == null)
        {
            return null;
        }

        // Try to select a source bundle record that is already
        // installed locally.
        for (int i = 0; i < sources.length; i++)
        {
            String updateLocation = (String)
                sources[i].getAttribute(BundleRecord.BUNDLE_UPDATELOCATION);
            if (updateLocation != null)
            {
                Bundle bundle = findLocalBundleByUpdate(updateLocation);
                if (bundle != null)
                {
                    return sources[i];
                }
            }
        }

        // If none of the sources are installed locally, then
        // just pick the first one.
        return sources[0];
    }

    /**
     * Returns an array of bundle records that resolve the supplied
     * package declaration.
     * @param target the package declaration to resolve.
     * @return an array of bundle records that resolve the package
     *         declaration or <tt>null</tt> if none are found.
    **/
    private synchronized BundleRecord[] findResolvingBundles(PackageDeclaration targetPkg)
    {
        // Create a list for storing bundles that can resolve package.
        ArrayList resolveList = new ArrayList();
        // Get the exporter list from the export package map for this package.
        ArrayList exporterList = (ArrayList) m_exportPackageMap.get(targetPkg.getName());
        // Loop through each exporter and see if it satisfies the target.
        for (int i = 0; (exporterList != null) && (i < exporterList.size()); i++)
        {
            // Get the export info from the exporter list.
            Object[] exportInfo = (Object[]) exporterList.get(i);
            // Get the export package from the export info.
            PackageDeclaration exportPkg = (PackageDeclaration)
                exportInfo[EXPORT_PACKAGE_IDX];
            // Get the export bundle from the export info.
            BundleRecord exportBundle = (BundleRecord)
                exportInfo[EXPORT_BUNDLE_IDX];
            // See if the export package satisfies the target package.
            if (exportPkg.doesSatisfy(targetPkg))
            {
                // Add it to the list of resolving bundles.
                resolveList.add(exportBundle);
            }
        }
        // If no resolving bundles were found, return null.
        if (resolveList.size() == 0)
        {
            return null;
        }
        // Otherwise, return an array containing resolving bundles.
        return (BundleRecord[]) resolveList.toArray(new BundleRecord[resolveList.size()]);
    }

    private void initialize()
    {
        m_initialized = true;
        m_bundleList.clear();
        m_exportPackageMap.clear();

        for (int urlIdx = 0; (m_urls != null) && (urlIdx < m_urls.length); urlIdx++)
        {
            parseRepositoryFile(m_hopCount, m_urls[urlIdx]);
        }
    }

    private String getUserAgentForBundle(Bundle bundle)
    {
      String uaName = bundle.getSymbolicName();
      String uaVers = (String) bundle.getHeaders().get("Bundle-Version");
      String uaComm = "";

      if (0==bundle.getBundleId()) { // The system bundle
        uaComm = uaVers;
        uaName = m_context.getProperty(Constants.FRAMEWORK_VENDOR);
        uaVers = m_context.getProperty(Constants.FRAMEWORK_VERSION);
      }
      return uaName
        + (uaVers!=null && uaVers.length()>0 ? ("/" + uaVers)      : "")
        + (uaComm!=null && uaComm.length()>0 ? (" (" +uaComm +")") : "");
    }

    private void parseRepositoryFile(int hopCount, String urlStr)
    {
        InputStream is = null;
        InputStreamReader isr = null;
        BufferedReader br = null;

        try {
            // Do it the manual way to have a chance to
            // set request properties as proxy auth (EW).
            URL url = new URL(urlStr);

            // This repository is no more. Silently ignore it.
            for (int i=0; i<noMoreHosts.length; i++) {
                if(noMoreHosts[i].equals(url.getHost())) {
                    return;
                }
            }

            URLConnection conn = url.openConnection();

            // Support for http proxy authentication
            String auth = m_context.getProperty("http.proxyAuth");
            if ((auth != null) && (auth.length() > 0))
            {
                if ("http".equals(url.getProtocol()) ||
                    "https".equals(url.getProtocol()))
                {
                    String base64 = Util.base64Encode(auth);
                    conn.setRequestProperty(
                        "Proxy-Authorization", "Basic " + base64);
                }
            }

            // Support for http basic authentication
            String basicAuth = m_context.getProperty("http.basicAuth");
            if (basicAuth != null && !"".equals(basicAuth)) {
              if ("http".equals(url.getProtocol()) ||
                  "https".equals(url.getProtocol())) {
                String base64 = Util.base64Encode(basicAuth);
                conn.setRequestProperty("Authorization",
                                        "Basic " +base64);
              }
            }

            // Identify us (via User-Agent) with bundlename and the
            // framework vendor name and version.
            conn.setRequestProperty("User-Agent",
                                    getUserAgentForBundle(m_context.getBundle())
                                    +" "
                                    +getUserAgentForBundle(m_context.getBundle(0))
                                    );
            is = conn.getInputStream();

            // Create the parser Kxml
            XmlCommonHandler handler = new XmlCommonHandler();
            handler.addType("bundles", ArrayList.class);
            handler.addType("repository", HashMap.class);
            handler.addType("extern-repositories", ArrayList.class);
            handler.addType("bundle", MultivalueMap.class);
            handler.addType("import-package", HashMap.class);
            handler.addType("export-package", HashMap.class);
            handler.setDefaultType(String.class);

            br = new BufferedReader(new InputStreamReader(is));
            KXmlSAXParser parser;
            parser = new KXmlSAXParser(br);
            try
            {
                parser.parseXML(handler);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                return;
            }

            List root = (List) handler.getRoot();
            for (int bundleIdx = 0; bundleIdx < root.size(); bundleIdx++)
            {
                Object obj = root.get(bundleIdx);

                // The elements of the root will either be a HashMap for
                // the repository tag or a MultivalueMap for the bundle
                // tag, as indicated above when we parsed the file.

                // If HashMap, then read repository information.
                if (obj instanceof HashMap)
                {
                    // Create a case-insensitive map.
                    Map repoMap = new TreeMap(new Comparator() {
                        public int compare(Object o1, Object o2)
                        {
                            return o1.toString().compareToIgnoreCase(o2.toString());
                        }
                    });
                    repoMap.putAll((Map) obj);

                    // Process external repositories if hop count is
                    // greater than zero.
                    if (hopCount > 0)
                    {
                        // Get the external repository list.
                        List externList = (List) repoMap.get(EXTERN_REPOSITORY_TAG);
                        for (int i = 0; (externList != null) && (i < externList.size()); i++)
                        {
                            parseRepositoryFile(hopCount - 1, (String) externList.get(i));
                        }
                    }
                }
                // Else if mulitvalue map, then create a bundle record
                // for the associated bundle meta-data.
                else if (obj instanceof MultivalueMap)
                {
                    // Create a case-insensitive map.
                    Map bundleMap = new TreeMap(new Comparator() {
                        public int compare(Object o1, Object o2)
                        {
                            return o1.toString().compareToIgnoreCase(o2.toString());
                        }
                    });
                    bundleMap.putAll((Map) obj);

                    // Convert any import package declarations
                    // to PackageDeclaration objects.
                    Object target = bundleMap.get(BundleRecord.IMPORT_PACKAGE);
                    if (target != null)
                    {
                        // Overwrite the original package declarations.
                        bundleMap.put(
                            BundleRecord.IMPORT_PACKAGE,
                            convertPackageDeclarations(target));
                    }

                    // Convert any export package declarations
                    // to PackageDeclaration objects.
                    target = bundleMap.get(BundleRecord.EXPORT_PACKAGE);
                    if (target != null)
                    {
                        // Overwrite the original package declarations.
                        bundleMap.put(
                            BundleRecord.EXPORT_PACKAGE,
                            convertPackageDeclarations(target));
                    }

                    // Create a bundle record using the map.
                    BundleRecord record = new BundleRecord(bundleMap);

                    // Try to put all exported packages from this bundle
                    // into the export package map to simplify access.
                    try
                    {
                        PackageDeclaration[] exportPkgs = (PackageDeclaration[])
                            record.getAttribute(BundleRecord.EXPORT_PACKAGE);
                        for (int exportIdx = 0;
                            (exportPkgs != null) && (exportIdx < exportPkgs.length);
                            exportIdx++)
                        {
                            // Check to see if this package is already in the
                            // export package map.
                            ArrayList exporterList = (ArrayList)
                                m_exportPackageMap.get(exportPkgs[exportIdx].getName());
                            // If the package is not in the map, create an
                            // array list for it.
                            if (exporterList == null)
                            {
                                exporterList = new ArrayList();
                            }
                            // Add the export info to the array list.
                            Object[] exportInfo = new Object[2];
                            exportInfo[EXPORT_PACKAGE_IDX] = exportPkgs[exportIdx];
                            exportInfo[EXPORT_BUNDLE_IDX] = record;
                            exporterList.add(exportInfo);
                            // Put the array list containing the export info
                            // into the export map, which will make it easy
                            // to search for which bundles export what. Note,
                            // if the exporterList already was in the map, this
                            // will just overwrite it with the same value.
                            m_exportPackageMap.put(
                                exportPkgs[exportIdx].getName(), exporterList);
                        }
                        // TODO: Filter duplicates.
                        m_bundleList.add(record);
                    }
                    catch (IllegalArgumentException ex)
                    {
                        // Ignore.
                    }
                }
            }
        }
        catch (MalformedURLException ex)
        {
            System.err.println("Error malformed URL, '" +urlStr +"': " + ex);
        }
        catch (IOException ex)
        {
            System.err.println("Error when conncting to '" +urlStr +"': " +ex);
        }
        finally
        {
            try
            {
                if (is != null) is.close();
            }
            catch (IOException ex)
            {
                // Not much we can do.
            }
        }

        Collections.sort(m_bundleList, new Comparator() {
            public int compare(Object o1, Object o2)
            {
                BundleRecord r1 = (BundleRecord) o1;
                BundleRecord r2 = (BundleRecord) o2;
                String name1 = (String) r1.getAttribute(BundleRecord.BUNDLE_NAME);
                String name2 = (String) r2.getAttribute(BundleRecord.BUNDLE_NAME);
                return name1.compareToIgnoreCase(name2);
            }
        });
    }

    private PackageDeclaration[] convertPackageDeclarations(Object target)
    {
        PackageDeclaration[] decls = null;

        // If there is only one package it will be a
        // Map as specified above when parsing.
        if (target instanceof Map)
        {
            // Put package declaration into an array.
            decls = new PackageDeclaration[1];
            decls[0] = convertPackageMap((Map) target);
        }
        // If there is more than one package, then the
        // MultivalueMap will convert them to a list of maps.
        else if (target instanceof List)
        {
            List pkgList = (List) target;
            decls = new PackageDeclaration[pkgList.size()];
            for (int pkgIdx = 0; pkgIdx < decls.length; pkgIdx++)
            {
                decls[pkgIdx] = convertPackageMap(
                    (Map) pkgList.get(pkgIdx));
            }
        }

        return decls;
    }

    private PackageDeclaration convertPackageMap(Map map)
    {
        // Create a case-insensitive map.
        Map pkgMap = new TreeMap(new Comparator() {
            public int compare(Object o1, Object o2)
            {
                return o1.toString().compareToIgnoreCase(o2.toString());
            }
        });
        pkgMap.putAll((Map) map);

        // Get package name and version.
        String name = (String) pkgMap.get(PackageDeclaration.PACKAGE_ATTR);
        String version = (String) pkgMap.get(PackageDeclaration.VERSION_ATTR);

        if (name != null)
        {
            return new PackageDeclaration(name, version);
        }

        return null;
    }
}
