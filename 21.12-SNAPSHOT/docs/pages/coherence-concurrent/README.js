<doc-view>

<h2 id="_distributed_concurrency">Distributed Concurrency</h2>
<div class="section">
<p>Coherence Concurrent module provides distributed implementations of the concurrency primitives from the <code>java.util.concurrent</code> package that you are already familiar with, such as executors, atomics, locks, semaphores and latches.</p>

<p>This allows you to implement concurrent applications using the constructs you are already familiar with, but to expand the scope of "concurrency" from a single process to potentially hundreds of processes within a Coherence cluster. You can use executors to submit tasks to be executed somewhere in the cluster; you can use locks, latches and semaphores to synchronize execution across many processes; you can use atomics to implement global counters across many processes, etc.</p>

<p>Please keep in mind that while these features are extremely powerful and allow you to reuse the knowledge you already have, they may have detrimental effect on scalability and/or performance. Whenever you synchronize execution via locks, latches or semaphores, you are introducing a potential bottleneck into the architecture. Whenever you use a distributed atomic to implement a global counter, you are turning very simple operations that take mere nanoseconds locally, such as increment and decrement, into fairly expensive network calls that could take milliseconds (and potentially block even longer under heavy load).</p>

<p>So, use these features when you really need them, but don&#8217;t go overboard: in many cases there is a better, faster and more scalable way to accomplish the same goal using Coherence primitives such as entry processors, aggregators and events, which were designed to perform and scale well in a distributed environment from the get go.</p>


<h3 id="_factory_classes">Factory Classes</h3>
<div class="section">
<p>Each of the features above is backed by one or more Coherence caches, possibly with preconfigured interceptors, but for the most part you shouldn&#8217;t care about that: all interaction with lower level Coherence primitives is hidden behind various factory classes that allow you to get the instances of the classes you need.</p>

<p>For example, you will use factory methods within <code>Atomics</code> class to get instances of various atomic types, <code>Locks</code> to get lock instances, <code>Latches</code> and <code>Semaphores</code> to get, well, latches and semaphores.</p>

</div>

<h3 id="_local_vs_remote">Local vs Remote</h3>
<div class="section">
<p>In many cases the factory classes will allow you to get both the <strong>local</strong> and the <strong>remote</strong> instances of various constructs. For example, <code>Locks.localLock</code> will give you an instance of a standard <code>java.util.concurrent.locks.ReentrantLock</code>, while <code>Locks.remoteLock</code> will return an instance of a <code>DistributedLock</code>.</p>

<p>The main advantage of using factory classes to construct both the local and the remote lock instances (in this case) is that it allows you to name local locks the same way you have to name distributed locks: calling <code>Locks.localLock("foo")</code> will always return the same <code>Lock</code> instance, as the <code>Locks</code> class internally caches both the local and the remote instances it created. Of course, in the case of remote locks, every locally cached remote lock instance is ultimately backed by a shared lock instance somewhere in the cluster, which is used to synchronize lock state across the processes.</p>

</div>

<h3 id="_serialization">Serialization</h3>
<div class="section">
<p>Coherence Concurrent supports both Java serialization and POF out-of-the-box, with Java serialization being the default.</p>

<p>If you want to use POF instead, you will need to specify that by setting <code>coherence.concurrent.serializer</code> system property to <code>pof</code>. You will also need to include <code>coherence-concurrent-pof-config.xml</code> into your own POF configuration file, in order to register built-in Coherence Concurrent types.</p>

</div>

<h3 id="_persistence">Persistence</h3>
<div class="section">
<p>Coherence Concurrent supports both active and on-demand persistence, but just like in the rest of Coherence it is set to <code>on-demand</code> by default.</p>

<p>In order to use active persistence you should set <code>coherence.concurrent.persistence</code> system property to <code>active</code>.</p>

</div>
</div>

<h2 id="_usage">Usage</h2>
<div class="section">
<p>In order to use Coherence Concurrent features, you need to declare it as a dependency in your <code>pom.xml</code>:</p>

<markup
lang="xml"

>    &lt;dependency&gt;
        &lt;groupId&gt;com.oracle.coherence.ce&lt;/groupId&gt;
        &lt;artifactId&gt;coherence-concurrent&lt;/artifactId&gt;
        &lt;version&gt;21.12-SNAPSHOT&lt;/version&gt;
    &lt;/dependency&gt;</markup>

<p>Once the necessary dependency is in place, you can start using the features it provides, as the following sections describe.</p>

<ul class="ulist">
<li>
<p><router-link to="#executors" @click.native="this.scrollFix('#executors')">Executors</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#executors-tbd" @click.native="this.scrollFix('#executors-tbd')">TBD (Ryan to expand/add sections)</router-link></p>

</li>
<li>
<p><router-link to="#cdi-executors" @click.native="this.scrollFix('#cdi-executors')">CDI Support for Executors</router-link></p>

</li>
</ul>
</li>
<li>
<p><router-link to="#atomics" @click.native="this.scrollFix('#atomics')">Atomics</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#atomics-primitive" @click.native="this.scrollFix('#atomics-primitive')">Primitive Types</router-link></p>

</li>
<li>
<p><router-link to="#atomics-reference" @click.native="this.scrollFix('#atomics-reference')">Reference Types</router-link></p>

</li>
<li>
<p><router-link to="#atomics-async" @click.native="this.scrollFix('#atomics-async')">Asynchronous Implementations</router-link></p>

</li>
<li>
<p><router-link to="#cdi-atomics" @click.native="this.scrollFix('#cdi-atomics')">CDI Support for Atomics</router-link></p>

</li>
</ul>
</li>
<li>
<p><router-link to="#locks" @click.native="this.scrollFix('#locks')">Locks</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#exclusive-locks" @click.native="this.scrollFix('#exclusive-locks')">Exclusive Locks</router-link></p>

</li>
<li>
<p><router-link to="#read-write-locks" @click.native="this.scrollFix('#read-write-locks')">Read/Write Locks</router-link></p>

</li>
<li>
<p><router-link to="#cdi-locks" @click.native="this.scrollFix('#cdi-locks')">CDI Support for Locks</router-link></p>

</li>
</ul>
</li>
<li>
<p><router-link to="#latches-semaphores" @click.native="this.scrollFix('#latches-semaphores')">Latches and Semaphores</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#count-down-latch" @click.native="this.scrollFix('#count-down-latch')">Count Down Latch</router-link></p>

</li>
<li>
<p><router-link to="#semaphore" @click.native="this.scrollFix('#semaphore')">Semaphore</router-link></p>

</li>
<li>
<p><router-link to="#cdi-latches-semaphores" @click.native="this.scrollFix('#cdi-latches-semaphores')">CDI Support for Latches and Semaphores</router-link></p>

</li>
</ul>
</li>
</ul>

<h3 id="executors">Executors</h3>
<div class="section">

<h4 id="executors-tbd">TBD (Ryan to expand/add sections)</h4>
<div class="section">

</div>

<h4 id="cdi-executors">CDI Support</h4>
<div class="section">

</div>
</div>

<h3 id="atomics">Atomics</h3>
<div class="section">

<h4 id="atomics-primitive">Primitive Types</h4>
<div class="section">

</div>

<h4 id="atomics-reference">Reference Types</h4>
<div class="section">

</div>

<h4 id="atomics-async">Asynchronous Implementations</h4>
<div class="section">

</div>

<h4 id="cdi-atomics">CDI Support</h4>
<div class="section">

</div>
</div>

<h3 id="locks">Locks</h3>
<div class="section">

<h4 id="exclusive-locks">Exclusive Locks</h4>
<div class="section">

</div>

<h4 id="read-write-locks">Read/Write Locks</h4>
<div class="section">

</div>

<h4 id="cdi-locks">CDI Support</h4>
<div class="section">

</div>
</div>

<h3 id="latches-semaphores">Latches and Semaphores</h3>
<div class="section">

<h4 id="count-down-latch">Count Down Latch</h4>
<div class="section">

</div>

<h4 id="semaphore">Semaphore</h4>
<div class="section">

</div>

<h4 id="cdi-latches-semaphores">CDI Support</h4>
<div class="section">

</div>
</div>
</div>
</doc-view>
