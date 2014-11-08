package vr4java;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
import org.eclipse.aether.repository.RepositoryPolicy;
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

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Created by tlaukkan on 11/6/2014.
 */
public class VrInstaller {

    public static void main(final String[] args) throws Exception{
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
        session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS);

        final Artifact artifactVersionRange = new DefaultArtifact(groupId + ":" + artifactId + ":[0,)");

        final VersionRangeRequest rangeRequest = new VersionRangeRequest();
        rangeRequest.setArtifact(artifactVersionRange);
        rangeRequest.setRepositories(remoteRepositories);

        final VersionRangeResult rangeResult = system.resolveVersionRange( session, rangeRequest );

        final Version newestVersion = rangeResult.getHighestVersion();

        final String version = newestVersion.toString();
        System.out.println( "Newest version " + version + " from repository "
                + rangeResult.getRepository( newestVersion ));

        final String artifactFullId = groupId + ":" + artifactId + ":" + version;
        final Artifact artifact = new DefaultArtifact(artifactFullId);

        final DependencyFilter classpathFilter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot( new Dependency( artifact, JavaScopes.COMPILE ) );
        collectRequest.setRepositories(remoteRepositories);
        final DependencyRequest dependencyRequest = new DependencyRequest( collectRequest, classpathFilter );

        session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_DAILY);
        final List<ArtifactResult> artifactResults =
                system.resolveDependencies( session, dependencyRequest ).getArtifactResults();

        File mainArtifactFile = null;
        for ( ArtifactResult artifactResult : artifactResults )
        {
            final File repositoryFile = artifactResult.getArtifact().getFile();
            final File destinationFile;

            if (groupId.equals(artifactResult.getArtifact().getGroupId()) &&
                    artifactId.equals(artifactResult.getArtifact().getArtifactId()) &&
                    version.equals(artifactResult.getArtifact().getVersion())) {
                destinationFile = new File(repositoryFile.getName());
                mainArtifactFile = destinationFile;
            } else {
                destinationFile = new File("lib/" + repositoryFile.getName());
            }

            //System.out.println(artifactResult.getArtifact() + " resolved to " + destinationFile + "(" + repositoryFile + ")");

            if (!destinationFile.exists() || destinationFile.lastModified() != repositoryFile.lastModified() ||
                    destinationFile.length() != repositoryFile.length()) {
                FileUtils.copyFile(repositoryFile, destinationFile);
                System.out.println("Copied from repository: " + destinationFile);
            }
        }
        System.out.println("Main artifact file: " + mainArtifactFile);

        final Process vrClientProcess = Runtime.getRuntime().exec("java -jar " + mainArtifactFile);
    }

}
