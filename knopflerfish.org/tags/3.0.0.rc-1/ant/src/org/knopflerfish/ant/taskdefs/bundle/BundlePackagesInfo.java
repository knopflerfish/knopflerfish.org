/*
 * Copyright (c) 2003-2009, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * - Neither the name of the KNOPFLERFISH project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.knopflerfish.ant.taskdefs.bundle;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * Class that holds the results of the Java package analysis of all
 * classes in a bundle.
 * <p>
 * Class and package names should use either '/' or '.' as separator
 * between package levels during build up phase. When all classes
 * packages have been added, make a call to {@link #toJavaNames()} to
 * convert all class / packages names using the internal Java
 * representation with '/' as separator to their non-internal
 * representation with '.' as separator.
 * Mixing of separator kinds is not supported!
 * </p>
 * <p>
 * When all classes ahve been added, before using the package using
 * map a call to {@link #postProcessUsingMap(Set,Set)} should be done.
 * </p>
 */
public class BundlePackagesInfo {

  /**
   * Get package name of class string representation.
   *
   * @param className A fully qualified class name.
   * @return The Java package name that the named class belongs
   *         to. Will return the empty string if the named class does
   *         not belong to any package.
   */
  public static String packageName(final String className) {
    String s = className.trim();
    int ix = s.lastIndexOf('/');
    if(ix == -1) {
      ix = s.lastIndexOf('.');
    }
    if (ix != -1) {
      s = s.substring(0, ix);
    } else {
      s = "";
    }
    return s;
  }


  /**
   * The set of classes provided by the bundle.
   * The elements of the set are the fully qualified class name.
   */
  private final SortedSet/*<String>*/ providedClasses = new TreeSet();


  /**
   * Adds a named class to the set of classes provided by this
   * bundle.
   *
   * This method also adds the package of the class to the set of
   * provided packages. It also add a reference to the class from its
   * own package.
   *
   * @param className the name of the class to add.
   * @return the name of the Java package that the given class belongs
   * to.
   */
  public String addProvidedClass(final String className)
  {
    final String pkgName = packageName(className);
    providedClasses.add(className);
    addProvidedPackage(pkgName);
    addReferencedClass(pkgName, className);

    return pkgName;
  }

  /**
   * Checks if a named class is provided by this bundle.
   * @param className the name of the class to check for.
   * @return <code>true</code> if the given class is in the set of
   *         classes provided by this bundle, <code>false</code>
   *         otherwise.
   */
  public boolean providesClass(final String className)
  {
    return providedClasses.contains(className);
  }


  /**
   * The sub set of the provided classes that implements the interface
   * {@link org.osgi.framework.BundleActivator}.
   */
  private final SortedSet/*<String>*/ activatorClasses = new TreeSet();

  /**
   * Adds a named class to the set of classes that implements the
   * interface {@link org.osgi.framework.BundleActivator}.
   *
   * @param className the name of the activator class to add.
   */
  public void addProvidedActivatorClass(final String className)
  {
    activatorClasses.add(className);
    providedClasses.add(className);
  }

  /**
   * Gets the cardinality of the set of provided bundle activator
   * classes.
   *
   * @return Number of elements in the set of provided bundle
   *         activator classes.
   */
  public int countProvidedActivatorClasses()
  {
    return activatorClasses.size();
  }

  /**
   * Checks if a named class is in the set of provided activator classes.
   * @param className the name of the activator class to check for.
   * @return <code>true</code> if the given class is in the set of
   *         provided activator classes, <code>false</code> otherwise.
   */
  public boolean providesActivatorClass(final String className)
  {
    return activatorClasses.contains(className);
  }

  /**
   * Return the set of provided activator classes as string suitable
   * for use in messages.
   * @return The provided set of activator classes as a string.
   */
  public String providedActivatorClassesAsString()
  {
    return activatorClasses.toString();
  }

  /**
   * Get the bundle activator from the set of provided bundle
   * activator classes.
   * @return The one and only activator class when the size of the set
   *         of provided bundle activator classes is <em>one</em>,
   *         otherwise <code>null</code>.
   */
  public String getActivatorClass()
  {
    return 1== activatorClasses.size()
      ? (String) activatorClasses.iterator().next()
      : (String) null;
  }


  /**
   * The set of packages that are provided by the classes in the
   * bundle.
   */
  private final SortedSet/*<String>*/ providedPackages = new TreeSet();

  /**
   * Adds a named package to the set of packages provided by this
   * bundle.
   * @param packageName the name of the Java package to add.
   */
  public void addProvidedPackage(final String packageName)
  {
    if(packageName == null || "".equals(packageName)) {
      return;
    }

    providedPackages.add(packageName);
  }

  /**
   * Checks if a named Java package is provided by this bundle.
   *
   * @param packageName the name of the package to check for.
   * @return <code>true</code> if the given package is in the set of
   *         packages provided by this bundle, <code>false</code>
   *         otherwise.
   */
  public boolean providesPackage(final String packageName)
  {
    return providedPackages.contains(packageName);
  }

  /**
   * Get a copy of the set of provided Java packages.
   *
   * @return A copy of the set of provided Java packages.
   */
  public SortedSet/*<String>*/ getProvidedPackages()
  {
    return new TreeSet(providedPackages);
  }

  /**
   * Gets the cardinality of the set of provided Java packages.
   *
   * @return Number of elements in the set of provided packages.
   */
  public int countProvidedPackages()
  {
    return providedPackages.size();
  }


  /**
   * The set of classes that are referenced from the provided classes.
   * I.e., classes that are used somehow by the provided classes.
   */
  private final SortedSet/*<String>*/ referencedClasses = new TreeSet();

  /**
   * The set of Java packages that are referenced from the provided
   * classes.
   */
  private final SortedSet/*<String>*/ referencedPackages = new TreeSet();

  /**
   * Get a copy of the set of referenced Java classes.
   *
   * @return A copy of the set of referenced Java packages.
   */
  public SortedSet/*<String>*/ getReferencedClasses()
  {
    return new TreeSet(referencedClasses);
  }


  /**
   * Get a the set of Java packages that are referenced by this bundle
   * but not provided by it.
   *
   * @return The set of un-provided referenced Java packages.
   */
  public SortedSet/*<String>*/ getUnprovidedReferencedPackages()
  {
    SortedSet res = new TreeSet(referencedPackages);
    res.removeAll(providedPackages);

    return res;
  }


  /**
   * Get a copy of the set of Java packages that are referenced by
   * this bundle.
   *
   * @return The set of referenced Java packages.
   */
  public SortedSet/*<String>*/ getReferencedPackages()
  {
    return new TreeSet(referencedPackages);
  }


  /**
   * A mapping from a provided Java package name to the set of Java
   * package names referenced by the classes in that provided package.
   */
  private final Map/*<String,Set<String>>*/
    packageToReferencedPackages = new HashMap();


  /**
   * Add a reference to a named class from some class in the
   * referencing Java package.
   *
   * If the given referenced class is an inner class, the we also add
   * a reference for its outer class. This is not really needed for
   * static inner classes, but there is no way to detect that on this
   * level.
   *
   * @param referencingPackage The Java package of the class having a
   *                           reference to <tt>className</tt>.
   * @param referencedClass Fully qualified name of the referenced
   *                        class.
   */
  public void addReferencedClass(final String referencingPackage,
                                 final String referencedClass)
  {
    if(null==referencedClass || 0==referencedClass.length()) {
      return;
    }

    final String referencedPackage = packageName(referencedClass);
    if("".equals(referencedPackage)) {
      // Referenced class is in the default package; skip it.
      return;
    }

    referencedClasses.add(referencedClass);
    final int dollarIdx = referencedClass.indexOf('$');
    if (-1<dollarIdx) {
      // This is an inner class add its outer class as well.
      referencedClasses.add(referencedClass.substring(0, dollarIdx));
    }
    referencedPackages.add(referencedPackage);
    if (null!=referencingPackage && 0<referencingPackage.length()) {
      SortedSet using = (SortedSet)
        packageToReferencedPackages.get(referencingPackage);
      if (null==using) {
        using = new TreeSet();
        packageToReferencedPackages.put(referencingPackage, using);
      }
      using.add(referencedPackage);
    }
  }

  /**
   * Post process the package to referenced packages map.
   *
   * <ol>
   *   <li> Remove all self references.
   *   <li> Remove all references to "java.*".
   *   <li> Remove all packages in the remove from referenced set are
   *        removed from all referenced sets.
   *   <li> Retain all packages in the retain in referenced set. I.e.,
   *        the referenced sets will only contain packages present in
   *        this set.
   * </ol>
   *
   * @param removeFromReferencedSets Packages to remove
   * @param retainInReferencedSets Packages to retain.
   */
  public void postProcessUsingMap(Set removeFromReferencedSets,
                                  Set retainInReferencedSets)
  {
    for (Iterator it = packageToReferencedPackages.entrySet().iterator();
         it.hasNext(); ) {
      final Map.Entry entry = (Map.Entry) it.next();
      final SortedSet using = (SortedSet) entry.getValue();
      using.remove((String) entry.getKey());
      using.removeAll(removeFromReferencedSets);
      using.retainAll(retainInReferencedSets);
    }
  }

  /**
   * Get a the set of Java packages that are referenced by the
   * given Java package.
   *
   * @param  packageName The name of the Java package to get
   *                     referenced Java packages for.
   * @return The set of referenced Java packages.
   */
  public SortedSet/*<String>*/
    getPackagesReferencedFromPackage(final String packageName)
  {
    return (SortedSet) packageToReferencedPackages.get(packageName);
  }


  /**
   * Replaces all '/' in class and package names with '.' in all the
   * collections that this class is holding.
   */
  public void toJavaNames()
  {
    toJavaNames(providedClasses);
    toJavaNames(providedPackages);
    toJavaNames(activatorClasses);
    toJavaNames(referencedClasses);
    toJavaNames(referencedPackages);

    HashMap tmpMap = new HashMap();
    for (Iterator it = packageToReferencedPackages.entrySet().iterator();
         it.hasNext(); ) {
      final Map.Entry entry = (Map.Entry) it.next();
      final String    key   = (String) entry.getKey();
      final SortedSet using = (SortedSet) entry.getValue();
      toJavaNames(using);
      tmpMap.put(key.replace('/', '.'), using);
    }
    packageToReferencedPackages.clear();
    packageToReferencedPackages.putAll(tmpMap);
  }

  /**
   * Replaces all '/' in class and package names with '.' in all the
   * collections that this class is holding.
   *
   * @param set the set of names to process.
   */
  private void toJavaNames(SortedSet set)
  {
    TreeSet tmpSet = new TreeSet();
    for (Iterator it = set.iterator(); it.hasNext();) {
      String item = (String) it.next();
      tmpSet.add(item.replace('/', '.'));
    }
    set.clear();
    set.addAll(tmpSet);
  }


  public boolean equals(Object other)
  {
    if (!(other instanceof BundlePackagesInfo)) return false;
    BundlePackagesInfo otherBpInfo = (BundlePackagesInfo) other;

    if (!providedPackages.equals(otherBpInfo.providedPackages)) {
      System.out.println("Diff for provided packages: mine="
                         +providedPackages
                         +", other=" +otherBpInfo.providedPackages);
      return false;
    }

    if (!providedClasses.equals(otherBpInfo.providedClasses)) {
      System.out.println("Diff for provided classes: mine="
                         +providedClasses
                         +", other=" +otherBpInfo.providedClasses);
      return false;
    }

    if (!activatorClasses.equals(otherBpInfo.activatorClasses)) {
      System.out.println("Diff for activator classes: mine="
                         +activatorClasses
                         +", other=" +otherBpInfo.activatorClasses);
      return false;
    }

    if (!referencedPackages.equals(otherBpInfo.referencedPackages)) {
      System.out.println("Diff for referenced packages: mine="
                         +referencedPackages
                         +", other=" +otherBpInfo.referencedPackages);
      final SortedSet all = new TreeSet(referencedPackages);
      all.addAll(otherBpInfo.referencedPackages);
      final SortedSet tmp = new TreeSet(all);
      tmp.removeAll(referencedPackages);
      System.out.println(" Other extra referenced packages: " +tmp);

      tmp.addAll(all);
      tmp.removeAll(otherBpInfo.referencedPackages);
      System.out.println(" My extra referenced packages: " +tmp);

      return false;
    }

    if (!referencedClasses.equals(otherBpInfo.referencedClasses)) {
      System.out.println("Diff for referenced classes: mine="
                         +referencedClasses
                         +", other=" +otherBpInfo.referencedClasses);

      final SortedSet all = new TreeSet(referencedClasses);
      all.addAll(otherBpInfo.referencedClasses);
      final SortedSet tmp = new TreeSet(all);
      tmp.removeAll(referencedClasses);
      System.out.println(" Other extra referenced classes: " +tmp);

      tmp.addAll(all);
      tmp.removeAll(otherBpInfo.referencedClasses);
      System.out.println(" My extra referenced classes: " +tmp);

      return false;
    }

    return true;
  }


  public String toString()
  {
    StringBuffer res = new StringBuffer(200);
    res.append("BundlePackagesInfo:\n\t");
    res.append("Provided packages: ").append(providedPackages).append("\n\t");
    res.append("Provided classes: ").append(providedClasses).append("\n\t");
    res.append("Provided Activators: ").append(activatorClasses).append("\n\t");

    res.append("Referenced packages: ").append(referencedPackages).append("\n\t");
    res.append("Referenced classes: ").append(referencedClasses).append("\n\t");

    res.append("Using map: ").append(packageToReferencedPackages).append("\n");

    return res.toString();
  }


}
