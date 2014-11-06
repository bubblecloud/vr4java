package vr4java;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Created by tlaukkan on 11/6/2014.
 */
public class InstallerTest {

    public static void main(final String[] args) throws Exception{

        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService( RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class );
        locator.addService( TransporterFactory.class, FileTransporterFactory.class );
        locator.addService( TransporterFactory.class, HttpTransporterFactory.class );

        locator.setErrorHandler( new DefaultServiceLocator.ErrorHandler()
        {
            @Override
            public void serviceCreationFailed( Class<?> type, Class<?> impl, Throwable exception )
            {
                exception.printStackTrace();
            }
        } );

        RepositorySystem system = locator.getService(RepositorySystem.class);

        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        final LocalRepository localRepo = new LocalRepository( "target/lib" );
        session.setLocalRepositoryManager( system.newLocalRepositoryManager( session, localRepo ) );

        session.setTransferListener( new ConsoleTransferListener() );
        session.setRepositoryListener( new ConsoleRepositoryListener() );

        Artifact artifact = new DefaultArtifact( "org.bubblecloud.vr4java:vr4java-client:1.0.2" );

        DependencyFilter classpathFlter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);

        Dependency dependency = new Dependency( artifact, JavaScopes.COMPILE );
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot( new Dependency( artifact, JavaScopes.COMPILE ) );
        collectRequest.setRepositories(
                Arrays.asList(
                        new RemoteRepository.Builder("central", "default", "http://central.maven.org/maven2/").build(),
                        new RemoteRepository.Builder("bubblecloud", "default", "http://repository-bubblecloud.forge.cloudbees.com/release/").build())

        );
        DependencyRequest dependencyRequest = new DependencyRequest( collectRequest, classpathFlter );

        CollectResult collectResult = system.collectDependencies( session, collectRequest );

        List<ArtifactResult> artifactResults =
                system.resolveDependencies( session, dependencyRequest ).getArtifactResults();

        for ( ArtifactResult artifactResult : artifactResults )
        {
            System.out.println( artifactResult.getArtifact() + " resolved to " + artifactResult.getArtifact().getFile() );
        }
    }

}
