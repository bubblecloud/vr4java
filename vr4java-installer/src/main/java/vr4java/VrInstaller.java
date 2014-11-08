package vr4java;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.version.Version;

import java.util.Arrays;
import java.util.List;

/**
 * Created by tlaukkan on 11/6/2014.
 */
public class VrInstaller {

    public static void main(final String[] args) throws Exception{
        // Configure logging.
        DOMConfigurator.configure("log4j.xml");

        final String groupId = "org.bubblecloud.vr4java";
        final String artifactId = "vr4java-client";

        final LocalRepository localRepository = new LocalRepository("repo");
        final List<RemoteRepository> remoteRepositories = Arrays.asList(
                new RemoteRepository.Builder("central", "default", "http://central.maven.org/maven2/").build(),
                new RemoteRepository.Builder("EclipseLink", "default", "http://download.eclipse.org/rt/eclipselink/maven.repo").build(),
                new RemoteRepository.Builder("bubblecloud", "default", "http://repository-bubblecloud.forge.cloudbees.com/release/").build());

        final DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService( RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class );
        locator.addService( TransporterFactory.class, FileTransporterFactory.class );
        locator.addService( TransporterFactory.class, HttpTransporterFactory.class );
        locator.setErrorHandler( new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed( Class<?> type, Class<?> impl, Throwable exception ) {
                System.out.println("Error creating " + impl + " :" + exception);
            }
        } );

        final RepositorySystem system = locator.getService(RepositorySystem.class);

        final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setLocalRepositoryManager( system.newLocalRepositoryManager( session, localRepository ) );
        session.setTransferListener( new ConsoleTransferListener() );
        session.setRepositoryListener( new ConsoleRepositoryListener() );

        final Artifact artifactVersionRange = new DefaultArtifact(groupId + ":" + artifactId + ":[0,)");

        final VersionRangeRequest rangeRequest = new VersionRangeRequest();
        rangeRequest.setArtifact(artifactVersionRange);
        rangeRequest.setRepositories(remoteRepositories);

        final VersionRangeResult rangeResult = system.resolveVersionRange( session, rangeRequest );

        final Version newestVersion = rangeResult.getHighestVersion();

        final String newestVersionString = newestVersion.toString();
        System.out.println( "Newest version " + newestVersionString + " from repository "
                + rangeResult.getRepository( newestVersion ));


        final Artifact artifact = new DefaultArtifact(groupId + ":" + artifactId + ":" + newestVersionString);

        final DependencyFilter classpathFilter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot( new Dependency( artifact, JavaScopes.COMPILE ) );
        collectRequest.setRepositories(remoteRepositories);
        DependencyRequest dependencyRequest = new DependencyRequest( collectRequest, classpathFilter );
        List<ArtifactResult> artifactResults =
                system.resolveDependencies( session, dependencyRequest ).getArtifactResults();

        for ( ArtifactResult artifactResult : artifactResults )
        {
            System.out.println( artifactResult.getArtifact() + " resolved to " + artifactResult.getArtifact().getFile() );
        }
    }

}
