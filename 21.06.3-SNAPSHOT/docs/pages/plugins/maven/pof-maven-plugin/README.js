<doc-view>

<h2 id="_pof_maven_plugin">POF Maven Plugin</h2>
<div class="section">
<p>The POF Maven Plugin provides automated instrumentation of classes with the <code>@PortableType</code> annotation
to generate consistent (and correct) implementations of Evolvable POF serialization methods.</p>

<p>It is a far from a trivial exercise to manually write serialization methods that support serializing
inheritance hierarchies that support the <code>Evolvable</code> concept. However, with static type analysis these methods
can be deterministically generated.</p>

<p>This allows developers to focus on business logic rather than implementing boilerplate code for the
above-mentioned methods.</p>

<div class="admonition note">
<p class="admonition-inline">Please see <router-link to="/docs/core/04_portable_types">Portable Types</router-link> documentation for more information and detailed instructions on Portable Types creation and usage.</p>
</div>
</div>

<h2 id="_usage">Usage</h2>
<div class="section">
<p>In order to use the POF Maven Plugin, you need to declare it as a plugin dependency in your <code>pom.xml</code>:</p>

<markup
lang="xml"

>  &lt;build&gt;
    &lt;plugins&gt;
      &lt;plugin&gt;
        &lt;groupId&gt;com.oracle.coherence.ce&lt;/groupId&gt;
        &lt;artifactId&gt;pof-maven-plugin&lt;/artifactId&gt;
        &lt;version&gt;21.06.3-SNAPSHOT&lt;/version&gt;
        &lt;executions&gt;
          &lt;execution&gt;
            &lt;id&gt;instrument&lt;/id&gt;
            &lt;goals&gt;
              &lt;goal&gt;instrument&lt;/goal&gt;
            &lt;/goals&gt;
          &lt;/execution&gt;
          &lt;execution&gt;
            &lt;id&gt;instrument-tests&lt;/id&gt;
            &lt;goals&gt;
              &lt;goal&gt;instrument-tests&lt;/goal&gt;
            &lt;/goals&gt;
          &lt;/execution&gt;
        &lt;/executions&gt;
      &lt;/plugin&gt;
    &lt;/plugins&gt;
  &lt;/build&gt;</markup>

<p>An example <code>Person</code> class (below) when processed with the plugin is below
results in the bytecode shown below.</p>

<markup
lang="java"

>@PortableType(id=1000)
public class Person
    {
    public Person()
        {
        }

    public Person(int id, String name, Address address)
        {
        super();
        this.id = id;
        this.name = name;
        this.address = address;
        }

    int id;
    String name;
    Address address;

    // getters and setters omitted for brevity
    }</markup>

<p>Generated bytecode:</p>

<markup
lang="bash"

>$ javap Person.class
Compiled from "Person.java"
public class demo.Person implements com.tangosol.io.pof.PortableObject,com.tangosol.io.pof.EvolvableObject {
  int id;
  java.lang.String name;
  demo.Address address;
  public demo.Person();
  public demo.Person(int, java.lang.String, demo.Address);
  public int getId();
  public void setId(int);
  public java.lang.String getName();
  public void setName(java.lang.String);
  public demo.Address getAddress();
  public void setAddress(demo.Address);
  public java.lang.String toString();
  public int hashCode();
  public boolean equals(java.lang.Object);

  public void readExternal(com.tangosol.io.pof.PofReader) throws java.io.IOException;   <span class="conum" data-value="1" />
  public void writeExternal(com.tangosol.io.pof.PofWriter) throws java.io.IOException;  <span class="conum" data-value="1" />
  public com.tangosol.io.Evolvable getEvolvable(int);                                   <span class="conum" data-value="1" />
  public com.tangosol.io.pof.EvolvableHolder getEvolvableHolder();                      <span class="conum" data-value="1" />
}</markup>

<ul class="colist">
<li data-value="1">Additional methods generated by Coherence POF plugin.</li>
</ul>
</div>
</doc-view>
